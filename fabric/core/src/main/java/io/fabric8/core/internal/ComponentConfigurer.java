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
package io.fabric8.core.internal;

import io.fabric8.core.internal.utils.ConfigInjectionUtils;
import io.fabric8.core.internal.utils.PlaceholderUtils;
import io.fabric8.core.internal.utils.StringUtils;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
@Service(ComponentConfigurer.class)
public class ComponentConfigurer extends AbstractComponent implements Configurer {

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }


    void bindRuntimeService(RuntimeService service) {
        this.runtimeService.bind(service);
    }

    void unbindRuntimeService(RuntimeService service) {
        this.runtimeService.unbind(service);
    }

    @Override
    public <T> void configure(Map<String, ?> configuration, T target) throws Exception {
        assertValid();

        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value.getClass().isArray()) {
                //do nothing
            } else if (value instanceof String) {
                String substitutedValue = PlaceholderUtils.substitute((String) value, (Map<String, String>) null);
                //We don't want to inject blanks. If substitution fails, do not inject.
                if (!StringUtils.isNullOrBlank(substitutedValue)) {
                    result.put(key, substitutedValue);
                }
            }
        }
        ConfigInjectionUtils.applyConfiguration(result, target);
    }
}
