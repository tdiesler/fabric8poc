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
import org.jboss.gravia.runtime.ServiceLocator;


/**
 * A profile builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilder extends ProfileBuilderBase<ProfileBuilder>, Builder<ProfileBuilder, Profile> {

    final class Factory {

        public static ProfileBuilder create() {
            ProfileBuilderFactory factory = ServiceLocator.getRequiredService(ProfileBuilderFactory.class);
            return factory.create();
        }

        public static ProfileBuilder create(String identity) {
            ProfileBuilderFactory factory = ServiceLocator.getRequiredService(ProfileBuilderFactory.class);
            return factory.create(identity);
        }

        public static ProfileBuilder createFrom(Version version, String identity) {
            ProfileBuilderFactory factory = ServiceLocator.getRequiredService(ProfileBuilderFactory.class);
            return factory.createFrom(version, identity);
        }

        public static ProfileBuilder createFrom(LinkedProfile linkedProfile) {
            ProfileBuilderFactory factory = ServiceLocator.getRequiredService(ProfileBuilderFactory.class);
            return factory.createFrom(linkedProfile);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
