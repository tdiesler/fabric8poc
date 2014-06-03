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
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.spi.utils.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

import org.jboss.gravia.process.spi.AbstractManagedProcess;
import org.jboss.gravia.runtime.spi.DefaultPropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The managed root container
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public abstract class AbstractManagedContainer<C extends ManagedCreateOptions> extends AbstractManagedProcess<C> implements ManagedContainer<C> {

    private final AttributeSupport attributes;
    private final ContainerIdentity identity;

    protected AbstractManagedContainer(C options) {
        super(options, new DefaultPropertiesProvider(new HashMap<String, Object>(), true, RuntimeService.DEFAULT_ENV_PREFIX));
        this.identity = options.getIdentity();
        this.attributes = new AttributeSupport(options.getAttributes(), true);
    }

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
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
        IllegalStateAssertion.assertTrue(getState() == null || getState() == State.CREATED, "Cannot put attribute in state: " + getState());
        return attributes.putAttribute(key, value);
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
        String jmxServiceURL = getAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL);
        IllegalStateAssertion.assertNotNull(jmxServiceURL, "Cannot obtain container attribute: JMX_SERVER_URL");
        return ManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    protected void populateProperties(Properties properties, CreateOptions options) {
        properties.put(BootConfiguration.VERSION, options.getVersion().toString());
        properties.put(BootConfiguration.PROFILE, StringUtils.join(getCreateOptions().getProfiles(), " "));
    }

    protected void configureZooKeeperServer(File confDir) throws IOException {
        // etc/io.fabric8.zookeeper.server-0000.cfg
        File zooKeeperServerFile = new File(confDir, "io.fabric8.zookeeper.server-0000.cfg");
        if (!getCreateOptions().isZooKeeperServer()) {
            zooKeeperServerFile.delete();
        }
    }

    protected void configureFabricBoot(File confDir, String comment) throws IOException {
        // etc/io.fabric8.boot.cfg
        File bootConfigFile = new File(confDir, "io.fabric8.boot.cfg");
        IllegalStateAssertion.assertTrue(bootConfigFile.exists(), "File does not exist: " + bootConfigFile);

        Properties props = new Properties();
        props.load(new FileReader(bootConfigFile));
        populateProperties(props, getCreateOptions());
        FileWriter fileWriter = new FileWriter(bootConfigFile);
        props.store(fileWriter, comment);
    }
}
