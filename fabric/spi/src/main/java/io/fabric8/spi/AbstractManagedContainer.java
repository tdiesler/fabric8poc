/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Constants;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.LifecycleException;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.management.remote.JMXConnector;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jboss.gravia.repository.DefaultMavenDelegateRepository;
import org.jboss.gravia.repository.MavenDelegateRepository;
import org.jboss.gravia.resource.MavenCoordinates;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceContent;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.runtime.spi.DefaultPropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * The managed root container
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public abstract class AbstractManagedContainer<C extends ManagedCreateOptions> implements ManagedContainer<C> {

    private final AttributeSupport attributes = new AttributeSupport();
    private final MavenDelegateRepository mavenRepository;
    private final ContainerIdentity identity;
    private final C createOptions;
    private File containerHome;
    private State state;
    private Process process;

    protected AbstractManagedContainer(C options) {
        IllegalArgumentAssertion.assertNotNull(options, "options");
        this.mavenRepository = new DefaultMavenDelegateRepository(new DefaultPropertiesProvider(new HashMap<String, Object>(), true, RuntimeService.DEFAULT_ENV_PREFIX));
        this.identity = options.getIdentity();
        this.attributes.putAllAttributes(options.getAttributes());
        this.createOptions = options;
    }

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
    }

    @Override
    public C getCreateOptions() {
        return createOptions;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributes.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    protected <T> T putAttribute(AttributeKey<T> key, T value) {
        IllegalStateAssertion.assertTrue(state == null || state == State.CREATED, "Cannot put attribute in state: " + state);
        return attributes.putAttribute(key, value);
    }

    @Override
    public final synchronized void create() {
        IllegalStateAssertion.assertTrue(state == null, "Cannot create container in state: " + state);

        File targetDir = getCreateOptions().getTargetDirectory();
        IllegalStateAssertion.assertTrue(targetDir.isDirectory() || targetDir.mkdirs(), "Cannot create target dir: " + targetDir);

        for (MavenCoordinates artefact : getCreateOptions().getMavenCoordinates()) {
            Resource resource = mavenRepository.findMavenResource(artefact);
            IllegalStateAssertion.assertNotNull(resource, "Cannot find maven resource: " + artefact);

            ResourceContent content = resource.adapt(ResourceContent.class);
            IllegalStateAssertion.assertNotNull(content, "Cannot obtain resource content for: " + artefact);

            try {
                ArchiveInputStream ais;
                if ("tar.gz".equals(artefact.getType())) {
                    InputStream inputStream = content.getContent();
                    ais = new TarArchiveInputStream(new GZIPInputStream(inputStream));
                } else {
                    InputStream inputStream = content.getContent();
                    ais = new ArchiveStreamFactory().createArchiveInputStream(artefact.getType(), inputStream);
                }
                ArchiveEntry entry = null;
                boolean needContainerHome = containerHome == null;
                while ((entry = ais.getNextEntry()) != null) {
                    File targetFile;
                    if (needContainerHome) {
                        targetFile = new File(targetDir, entry.getName());
                    } else {
                        targetFile = new File(containerHome, entry.getName());
                    }
                    if (!entry.isDirectory()) {
                        File parentDir = targetFile.getParentFile();
                        IllegalStateAssertion.assertTrue(parentDir.exists() || parentDir.mkdirs(), "Cannot create target directory: " + parentDir);

                        FileOutputStream fos = new FileOutputStream(targetFile);
                        IOUtils.copy(ais, fos);
                        fos.close();

                        if (needContainerHome && containerHome == null) {
                            File homeDir = parentDir;
                            while (!homeDir.getParentFile().equals(targetDir)) {
                                homeDir = homeDir.getParentFile();
                            }
                            containerHome = homeDir;
                        }
                    }
                }
                ais.close();
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot extract artefact: " + artefact, ex);
            }
        }

        state = State.CREATED;

        try {
            doConfigure();
        } catch (Exception ex) {
            throw new LifecycleException("Cannot configure container", ex);
        }
    }

    @Override
    public File getContainerHome() {
        return containerHome;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public final synchronized void start() {
        assertNotDestroyed();
        try {
            if (state == State.CREATED || state == State.STOPPED) {
                doStart();
                state = State.STARTED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot start container", ex);
        }
    }

    @Override
    public final synchronized void stop() {
        assertNotDestroyed();
        try {
            if (state == State.STARTED) {
                doStop();
                destroyProcess();
                state = State.STOPPED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot stop container", ex);
        }
    }

    @Override
    public final synchronized void destroy() {
        assertNotDestroyed();
        if (state == State.STARTED) {
            try {
                stop();
            } catch (Exception ex) {
                // ignore
            }
        }
        try {
            doDestroy();
        } catch (Exception ex) {
            throw new LifecycleException("Cannot destroy container", ex);
        } finally {
            state = State.DESTROYED;
        }
    }

    @Override
    public final JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        Map<String, Object> env = new HashMap<String, Object>();
        if (jmxUsername != null && jmxPassword != null) {
            String[] credentials = new String[] { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }
        return getJMXConnector(env, timeout, unit);
    }

    protected JMXConnector getJMXConnector(Map<String, Object> env, long timeout, TimeUnit unit) {
        String jmxServiceURL = getAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL);
        IllegalStateAssertion.assertNotNull(jmxServiceURL, "Cannot obtain container attribute: JMX_SERVER_URL");
        return ManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    private void assertNotDestroyed() {
        IllegalStateAssertion.assertFalse(state == State.DESTROYED, "Cannot start container in state: " + state);
    }

    protected void doConfigure() throws Exception {
    }

    protected void doStart() throws Exception {
    }

    protected void doStop() throws Exception {
    }

    protected void doDestroy() throws Exception {
    }

    protected void startProcess(ProcessBuilder processBuilder) throws IOException {
        process = processBuilder.start();
        new Thread(new ConsoleConsumer(process, getCreateOptions())).start();
    }

    protected void destroyProcess() throws Exception {
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
    }

    protected final int nextAvailablePort(int portValue) {
        return nextAvailablePort(portValue, null);
    }

    protected int nextAvailablePort(int portValue, InetAddress bindAddr) {
        PortManager portManager = ServiceLocator.getRequiredService(PortManager.class);
        return portManager.nextAvailablePort(portValue, bindAddr);
    }

    /**
     * Runnable that consumes the output of the process.
     * If nothing consumes the output the container may hang on some platforms
     */
    public static class ConsoleConsumer implements Runnable {

        private final Process process;
        private final ManagedCreateOptions options;

        public ConsoleConsumer(Process process, ManagedCreateOptions options) {
            this.process = process;
            this.options = options;
        }

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (options.isOutputToConsole())
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }
    }
}
