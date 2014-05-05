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
 * A builder for a profile version
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileVersionBuilder extends AttributableBuilder<ProfileVersionBuilder>, Builder<LinkedProfileVersion> {

    ProfileVersionBuilder identity(Version version);

    ProfileVersionBuilder fromOptionsProvider(OptionsProvider<ProfileVersionBuilder> optionsProvider);

    NestedProfileBuilder getProfileBuilder(String identity);

    ProfileVersionBuilder addProfile(Profile profile);

    ProfileVersionBuilder removeProfile(String identity);

    interface NestedProfileBuilder extends ProfileBuilderBase<NestedProfileBuilder>  {

        NestedProfileBuilder fromOptionsProvider(OptionsProvider<ProfileBuilder> optionsProvider);

        ProfileVersionBuilder and();
    }

    final class Factory {

        public static ProfileVersionBuilder create() {
            ProfileVersionBuilderFactory factory = ServiceLocator.awaitService(ProfileVersionBuilderFactory.class);
            return factory.create(null);
        }

        public static ProfileVersionBuilder create(Version version) {
            ProfileVersionBuilderFactory factory = ServiceLocator.awaitService(ProfileVersionBuilderFactory.class);
            return factory.create(version);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
