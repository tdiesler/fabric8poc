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

import java.util.Map;

import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.ServiceLocator;

/**
 * A profile builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilder extends AttributableBuilder<ProfileBuilder> {

    ProfileBuilder identity(ProfileIdentity identity);

    ProfileBuilder profileVersion(VersionIdentity version);

    ProfileBuilder addProfileItem(ProfileItem item);

    ProfileBuilder removeProfileItem(String identity);

    ProfileBuilder addConfigurationItem(String mergeIndex, Map<String, Object> config);

    ProfileBuilder addResourceItem(Resource resource);

    ProfileBuilder addSharedResourceItem(Resource resource);

    ProfileBuilder addReferenceResourceItem(Resource resource);

    ProfileBuilder addRequirementItem(Requirement requirement);

    ProfileBuilder addParentProfile(ProfileIdentity identity);

    ProfileBuilder removeParentProfile(ProfileIdentity identity);

    Profile getProfile();

    final class Factory {

        public static ProfileBuilder create() {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder();
        }

        public static ProfileBuilder create(String identity) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder(ProfileIdentity.createFrom(identity));
        }

        public static ProfileBuilder create(ProfileIdentity identity) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder(identity);
        }

        public static ProfileBuilder createFrom(VersionIdentity version, ProfileIdentity identity) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilderFrom(version, identity);
        }

        public static ProfileBuilder createFrom(LinkedProfile linkedProfile) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilderFrom(linkedProfile);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
