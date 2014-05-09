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

import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProfileVersionBuilderFactory;
import io.fabric8.spi.DefaultProfileVersionBuilder;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * A provider service for the {@link ProfileVersionBuilderFactory}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(service = { ProfileVersionBuilderFactory.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileVersionBuilderService extends AbstractComponent implements ProfileVersionBuilderFactory {

    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public ProfileVersionBuilder create() {
        assertValid();
        return new DefaultProfileVersionBuilder((Version) null);
    }

    @Override
    public ProfileVersionBuilder create(Version version) {
        assertValid();
        return new DefaultProfileVersionBuilder(version);
    }

    @Override
    public ProfileVersionBuilder createFrom(Version version) {
        assertValid();
        LinkedProfileVersion linkedVersion = profileService.get().getLinkedProfileVersion(version);
        return linkedVersion != null ? new DefaultProfileVersionBuilder(linkedVersion) : new DefaultProfileVersionBuilder(version);
    }

    @Override
    public ProfileVersionBuilder createFrom(LinkedProfileVersion linkedVersion) {
        assertValid();
        return new DefaultProfileVersionBuilder(linkedVersion);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
}
