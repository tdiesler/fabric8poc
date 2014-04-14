/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Constants;
import io.fabric8.api.container.ContainerConfiguration;
import io.fabric8.api.container.LifecycleException;
import io.fabric8.api.container.ManagedContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jboss.gravia.repository.DefaultMavenDelegateRepository;
import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.repository.MavenDelegateRepository;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceContent;
import org.jboss.gravia.runtime.spi.DefaultPropertiesProvider;

/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public abstract class AbstractManagedContainer<C extends ContainerConfiguration> implements ManagedContainer<C> {

    private final AttributeSupport attributeSupport = new AttributeSupport();
    private final MavenDelegateRepository mavenRepository;
    private C configuration;
    private File containerHome;
    private State state;
    private Process process;

    protected AbstractManagedContainer() {
        mavenRepository = new DefaultMavenDelegateRepository(new DefaultPropertiesProvider());
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributeSupport.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributeSupport.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributeSupport.hasAttribute(key);
    }

    protected <T> T putAttribute(AttributeKey<T> key, T value) {
        return attributeSupport.putAttribute(key, value);
    }

    protected <T> T removeAttribute(AttributeKey<T> key) {
        return attributeSupport.removeAttribute(key);
    }

    @Override
    public final synchronized void create(C configuration) throws LifecycleException {
        this.configuration = configuration;
        if (state != null)
            throw new IllegalStateException("Cannot create container in state: " + state);

        File targetdir = configuration.getTargetDirectory();
        if (!targetdir.isDirectory() && !targetdir.mkdirs())
            throw new IllegalStateException("Cannot create target dir: " + targetdir);

        for (MavenCoordinates artefact : configuration.getMavenCoordinates()) {
            Resource resource = mavenRepository.findMavenResource(artefact);
            if (resource == null)
                throw new IllegalStateException("Cannot find maven resource: " + artefact);

            ResourceContent resourceContent = resource.adapt(ResourceContent.class);
            if (resourceContent == null)
                throw new IllegalStateException("Cannot obtain resource content for: " + artefact);

            try {
                ArchiveInputStream ais;
                if ("tar.gz".equals(artefact.getType())) {
                    InputStream inputStream = resourceContent.getContent();
                    ais = new TarArchiveInputStream(new GZIPInputStream(inputStream));
                } else {
                    InputStream inputStream = resourceContent.getContent();
                    ais = new ArchiveStreamFactory().createArchiveInputStream(artefact.getType(), inputStream);
                }
                ArchiveEntry entry = null;
                while ((entry = ais.getNextEntry()) != null) {
                    File targetFile;
                    if (containerHome == null) {
                        targetFile = new File(targetdir, entry.getName());
                    } else {
                        targetFile = new File(containerHome, entry.getName());
                    }
                    if (!entry.isDirectory()) {
                        File targetDir = targetFile.getParentFile();
                        if (!targetDir.exists() && !targetDir.mkdirs()) {
                            throw new IllegalStateException("Cannot create target directory: " + targetDir);
                        }

                        FileOutputStream fos = new FileOutputStream(targetFile);
                        IOUtils.copy(ais, fos);
                        fos.close();
                    }
                }
                ais.close();
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot extract artefact: " + artefact, ex);
            }
            if (containerHome == null) {
                File[] childDirs = targetdir.listFiles();
                if (childDirs.length != 1)
                    throw new IllegalStateException("Expected one child directory, but was: " + Arrays.asList(childDirs));
                containerHome = childDirs[0];
            }
        }

        state = State.CREATED;

        try {
            doConfigure(configuration);
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
    public final synchronized void start() throws LifecycleException {
        assertNotDestroyed();
        try {
            if (state == State.CREATED || state == State.STOPPED) {
                doStart(configuration);
                state = State.STARTED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot start container", ex);
        }
    }

    @Override
    public final synchronized void stop() throws LifecycleException {
        assertNotDestroyed();
        try {
            if (state == State.STARTED) {
                doStop(configuration);
                destroyProcess();
                state = State.STOPPED;
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot stop container", ex);
        }
    }

    @Override
    public final synchronized void destroy() throws LifecycleException {
        assertNotDestroyed();
        if (state == State.STARTED) {
            try {
                stop();
            } catch (Exception ex) {
                // ignore
            }
        }
        try {
            doDestroy(configuration);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot destroy container", ex);
        } finally {
            state = State.DESTROYED;
        }
    }

    private void assertNotDestroyed() {
        if (state == State.DESTROYED)
            throw new IllegalStateException("Cannot start container in state: " + state);
    }

    protected void doConfigure(C configuration) throws Exception {
    }

    protected void doStart(C configuration) throws Exception {
    }

    protected void doStop(C configuration) throws Exception {
    }

    protected void doDestroy(C configuration) throws Exception {
    }

    protected void startProcess(ProcessBuilder processBuilder, ContainerConfiguration config) throws IOException {
        process = processBuilder.start();
        new Thread(new ConsoleConsumer(process, config)).start();
    }

    protected void destroyProcess() throws Exception {
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
    }

    @Override
    public JMXConnector getJMXConnector(String username, String password, long timeout, TimeUnit unit) {
        assertNotDestroyed();

        String jmxServiceURL = getAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL);
        if (jmxServiceURL == null)
            throw new IllegalStateException("Cannot obtain container attribute: JMX_SERVER_URL");

        JMXServiceURL serviceURL;
        try {
            serviceURL = new JMXServiceURL(jmxServiceURL);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }

        Map<String, Object> env = new HashMap<String, Object>();
        if (username != null && password != null) {
            String[] credentials = new String[] { username, password };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }

        Exception lastException = null;
        JMXConnector connector = null;
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (connector == null && System.currentTimeMillis() < end) {
            try {
                connector = JMXConnectorFactory.connect(serviceURL, env);
            } catch (Exception ioex) {
                lastException = ioex;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (connector == null) {
            throw new IllegalStateException("Cannot obtain JMXConnector", lastException);
        }
        return connector;
    }

    /**
     * Runnable that consumes the output of the process.
     * If nothing consumes the output the container may hang on some platforms
     */
    public static class ConsoleConsumer implements Runnable {

        private final Process process;
        private final ContainerConfiguration config;

        public ConsoleConsumer(Process process, ContainerConfiguration config) {
            this.process = process;
            this.config = config;
        }

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (config.isOutputToConsole())
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }
    }
}