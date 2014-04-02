/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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

import java.util.Set;

import org.jboss.gravia.resource.Version;


/**
 * The abstraction of a Fabric8 container
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Container extends AttributeSupport {

    enum State {
        CREATED, STARTED, STOPPED, DESTROYED
    }

    /**
     * Get the container identity
     */
    ContainerIdentity getIdentity();

    /**
     * Get the associated host
     */
    HostIdentity getHost();

    /**
     * Get the container state
     */
    State getState();

    /**
     * Get the list of child containers
     */
    Set<ContainerIdentity> getChildren();

    /**
     * Get the set of provided management domains
     */
    Set<String> getManagementDomains();

    /**
     * Get the set of available service endpoints
     */
    Set<ServiceEndpointIdentity> getServiceEndpoints();

    /**
     * Get the profile version
     */
    Version getVersion();

    /**
     * Get the associated list of profiles
     */
    Set<ProfileIdentity> getProfiles();

    /**
     * True if the profile with the given identity exists
     */
    boolean hasProfile(ProfileIdentity identity);

}
