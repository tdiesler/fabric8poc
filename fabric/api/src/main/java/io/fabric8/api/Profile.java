/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */
package io.fabric8.api;

import java.util.Set;

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
public interface Profile extends Attributable, Identifiable<ProfileIdentity> {

    /**
     * Get the associated profile version
     */
    Version getVersion();

    /**
     * Get the profile parents
     */
    Set<ProfileIdentity> getParents();

    /**
     * Get the profile item for the given name
     */
    <T extends ProfileItem> T getProfileItem(String identity, Class<T> type);

    /**
     * Get the set of profile items for the given type
     */
    <T extends ProfileItem> Set<T> getProfileItems(Class<T> type);

}
