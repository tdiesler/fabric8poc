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


/**
 * A profile that is linked to its parent profiles.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
public interface LinkedProfile extends Profile {

    /**
     * Get the linked parent profile.
     */
    LinkedProfile getLinkedParent(ProfileIdentity identity);

    /**
     * Get the linked parent profiles
     */
    Map<ProfileIdentity, LinkedProfile> getLinkedParents();

    /**
     * Get the effective profile
     */
    Profile getEffectiveProfile();
}
