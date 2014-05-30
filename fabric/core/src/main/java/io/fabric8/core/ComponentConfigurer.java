/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */
package io.fabric8.core;

import io.fabric8.core.utils.ConfigInjectionUtils;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.spi.CompositePropertiesProvider;
import org.jboss.gravia.runtime.spi.MapPropertiesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.SubstitutionPropertiesProvider;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
@Service(Configurer.class)
public class ComponentConfigurer extends AbstractComponent implements Configurer {

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public <T> Map<String, Object> configure(final Map<String, Object> configuration, T target, String... ignorePrefix) throws Exception {
        assertValid();
        Map<String, Object> result = new HashMap<>();
        final Runtime runtime = RuntimeLocator.getRuntime();
        PropertiesProvider provider = new SubstitutionPropertiesProvider(new CompositePropertiesProvider(new MapPropertiesProvider(configuration), new PropertiesProvider() {
            @Override
            public Object getProperty(String key) {
                return runtime.getProperty(key);
            }

            @Override
            public Object getProperty(String key, Object defaultValue) {
                return runtime.getProperty(key, defaultValue);
            }
        }));

        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = provider.getProperty(key);
            result.put(key, value);
        }
        ConfigInjectionUtils.applyConfiguration(result, target, ignorePrefix);
        return result;
    }
}
