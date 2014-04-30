/*
 * #%L
 * Fabric8 :: API
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
package io.fabric8.api;

import org.jboss.gravia.resource.Version;



/**
 * A builder for a fabric profile
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilder extends AttributableBuilder<ProfileBuilder, Profile> {

    ProfileBuilder identity(String identity);

    ProfileBuilder profileVersion(Version version);

    ProfileBuilder fromOptionsProvider(ProfileOptionsProvider optionsProvider);

    <T extends ProfileItemBuilder<?, ?>> T getProfileItemBuilder(String identity, Class<T> type);

    ProfileBuilder addProfileItem(ProfileItem item);

    ProfileBuilder removeProfileItem(String identity);

    ProfileBuilder addParentProfile(String identity);

    ProfileBuilder removeParentProfile(String identity);

    final class Factory {

        public static ProfileBuilder create() {
            ProfileBuilderFactory factory = ServiceLocator.awaitService(ProfileBuilderFactory.class);
            return factory.create();
        }

        public static ProfileBuilder create(String identity) {
            ProfileBuilderFactory factory = ServiceLocator.awaitService(ProfileBuilderFactory.class);
            return factory.create(identity);
        }

        public static ProfileBuilder createFrom(Version version, String identity) {
            ProfileBuilderFactory factory = ServiceLocator.awaitService(ProfileBuilderFactory.class);
            return factory.createFrom(version, identity);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
