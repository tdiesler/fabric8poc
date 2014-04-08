/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package io.fabric8.core.service;

import io.fabric8.core.api.ConfigurationProfileItem;
import io.fabric8.core.spi.scr.AbstractComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ConfigurationManager.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ConfigurationManager extends AbstractComponent {

    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    void applyConfigurationItems(Set<ConfigurationProfileItem> configItems) {
        assertValid();
        ProfileSupport.applyConfigurationItems(configAdmin.get(), configItems);
    }

    @Reference
    void bindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }
}