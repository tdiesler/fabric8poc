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
package io.fabric8.core.internal;

import io.fabric8.core.api.ConfigurationProfileItem;
import io.fabric8.core.api.Profile;
import io.fabric8.core.spi.ProfileService;
import io.fabric8.core.spi.scr.AbstractComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.io.IOException;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { BootstrapService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class BootstrapService extends AbstractComponent {

    private final ValidatingReference<ConfigurationManager> configManager = new ValidatingReference<ConfigurationManager>();
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
        Profile profile = profileService.get().getDefaultProfile();
        Set<ConfigurationProfileItem> items = profile.getProfileItems(ConfigurationProfileItem.class);
        configManager.get().applyConfigurationItems(items);
    }

    @Reference
    void bindConfigurationManager(ConfigurationManager service) {
        this.configManager.bind(service);
    }

    void unbindConfigurationManager(ConfigurationManager service) {
        this.configManager.unbind(service);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        this.profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        this.profileService.unbind(service);
    }
}
