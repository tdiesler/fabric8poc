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
package io.fabric8.spi.internal;

import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilders;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.DefaultConfigurationItemBuilder;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.DefaultProfileVersionBuilder;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * A provider service for the {@link ProfileBuilders}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component( policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileBuilders.class)
public final class ProfileBuildersImpl extends AbstractComponent implements ProfileBuilders {

    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<>();

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public ProfileVersionBuilder profileVersionBuilder() {
        assertValid();
        return new DefaultProfileVersionBuilder((VersionIdentity) null);
    }

    @Override
    public ProfileVersionBuilder profileVersionBuilder(VersionIdentity version) {
        assertValid();
        return new DefaultProfileVersionBuilder(version);
    }

    @Override
    public ProfileVersionBuilder profileVersionBuilderFrom(VersionIdentity version) {
        assertValid();
        LinkedProfileVersion linkedVersion = profileService.get().getLinkedProfileVersion(version);
        return linkedVersion != null ? new DefaultProfileVersionBuilder(linkedVersion) : new DefaultProfileVersionBuilder(version);
    }

    @Override
    public ProfileVersionBuilder profileVersionBuilderFrom(LinkedProfileVersion linkedVersion) {
        assertValid();
        return new DefaultProfileVersionBuilder(linkedVersion);
    }

    @Override
    public ProfileBuilder profileBuilder() {
        assertValid();
        return new DefaultProfileBuilder((ProfileIdentity) null);
    }

    @Override
    public ProfileBuilder profileBuilder(ProfileIdentity identity) {
        assertValid();
        return new DefaultProfileBuilder(identity);
    }

    @Override
    public ProfileBuilder profileBuilderFrom(VersionIdentity version, ProfileIdentity identity) {
        LinkedProfile linkedProfile = profileService.get().getLinkedProfile(version, identity);
        return new DefaultProfileBuilder(linkedProfile);
    }

    @Override
    public ProfileBuilder profileBuilderFrom(LinkedProfile linkedProfile) {
        return new DefaultProfileBuilder(linkedProfile);
    }

    @Override
    public ConfigurationItemBuilder configurationItemBuilder() {
        return new DefaultConfigurationItemBuilder();
    }

    @Override
    public ConfigurationItemBuilder configurationItemBuilder(String identity) {
        return new DefaultConfigurationItemBuilder(identity);
    }

    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
}
