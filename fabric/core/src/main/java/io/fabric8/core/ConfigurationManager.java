/*
 * #%L
 * Fabric8 :: Core
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
package io.fabric8.core;

import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an abstraction and convenience around {@link ConfigurationAdmin}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ConfigurationManager.class)
public final class ConfigurationManager extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    void applyConfiguration(String pid, Map<String, Object> nextConfig) {
        assertValid();
        LOGGER.info("Apply configuration: {}", pid);
        try {
            Configuration config = configAdmin.get().getConfiguration(pid, null);
            Map<String, Object> prevConfig = toMap(config.getProperties());
            if (needsUpdate(prevConfig, nextConfig)) {
                config.update(toDictionary(nextConfig));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot update configuration: " + pid, ex);
        }
    }

    private boolean needsUpdate(Map<String, Object> prevConfig, Map<String, Object> nextConfig) {
        prevConfig.remove(Constants.SERVICE_PID);
        nextConfig = new HashMap<String, Object>(nextConfig);
        nextConfig.remove(Constants.SERVICE_PID);
        return !nextConfig.equals(prevConfig);
    }

    private Dictionary<String, Object> toDictionary(Map<String, Object> config) {
        Dictionary<String, Object> result = new Hashtable<>();
        if (config != null) {
            for (Entry<String, Object> entry : config.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private Map<String, Object> toMap(Dictionary<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            Enumeration<String> keys = config.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                result.put(key, config.get(key));
            }
        }
        return result;
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }
}
