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
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilderFactory;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;

/**
 * A provider service for the {@link ProfileBuilderFactory}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component( policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileBuilderFactory.class)
public final class ProfileBuilderService extends AbstractComponent implements ProfileBuilderFactory {

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
    public ProfileBuilder createFrom(Version version, String identity) {
        LinkedProfile linkedProfile = profileService.get().getLinkedProfile(version, identity);
        return new DefaultProfileBuilder(linkedProfile);
    }

    @Override
    public ProfileBuilder createFrom(LinkedProfile linkedProfile) {
        return new DefaultProfileBuilder(linkedProfile);
    }

    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
}
