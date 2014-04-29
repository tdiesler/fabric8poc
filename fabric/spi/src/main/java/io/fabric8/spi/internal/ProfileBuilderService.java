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

import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilderFactory;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * A provider service for the {@link ProfileBuilderFactory}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(service = { ProfileBuilderFactory.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileBuilderService extends AbstractComponent implements ProfileBuilderFactory {

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
    public ProfileBuilder create() {
        assertValid();
        return new DefaultProfileBuilder((String) null);
    }

    @Override
    public ProfileBuilder create(String identity) {
        assertValid();
        return new DefaultProfileBuilder(identity);
    }

    @Override
    public ProfileBuilder createFrom(Profile profile) {
        LinkedProfile linkedProfile = profileService.get().copyProfile(profile);
        return new DefaultProfileBuilder(linkedProfile);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
}
