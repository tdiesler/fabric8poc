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
package io.fabric8.core;

import io.fabric8.api.ConfigurationItem;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { BootstrapService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class BootstrapService extends AbstractComponent {

    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void activateInternal() throws IOException {

        // Read the default profile and apply ConfigurationItems to ConfigurationAdmin
        ProfileState defaultProfile = profileService.get().getDefaultProfile();
        Set<ConfigurationItem> items = defaultProfile.getProfileItems(ConfigurationItem.class);
        ProfileSupport.applyConfigurationItems(configAdmin.get(), items);
    }

    @Reference
    void bindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        this.profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        this.profileService.unbind(service);
    }
}
