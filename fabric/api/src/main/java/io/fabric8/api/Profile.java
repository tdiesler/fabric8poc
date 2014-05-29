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

import java.util.List;

import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;


/**
 * The abstraction of a Fabric8 profile.
 *
 * <ol>
 * <li> How does profile templating work?
 * <li> How do container specific profiles work?
 * </ol>
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Profile extends Attributable, Identifiable<String> {

    String DEFAULT_PROFILE_IDENTITY = "default";

    /**
     * Get the associated profile version
     */
    Version getVersion();

    /**
     * Get the profile parents
     */
    List<String> getParents();

    /**
     * Get the profile item for the given name
     */
    <T extends ProfileItem> T getProfileItem(String identity, Class<T> type);

    /**
     * Get the profile item for the given resource identity
     */
    ResourceItem getProfileItem(ResourceIdentity identity);

    /**
     * Get the set of profile items for the given type
     */
    <T extends ProfileItem> List<T> getProfileItems(Class<T> type);

}
