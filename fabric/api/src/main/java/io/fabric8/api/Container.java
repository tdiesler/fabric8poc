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

import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * A fabric container
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Container extends Attributable, Identifiable<ContainerIdentity> {

    /**
     * The container states
     */
    enum State {
        CREATED, STARTED, STOPPED, DESTROYED
    }

    /**
     * The configuration PID for this service
     */
    String CONTAINER_SERVICE_PID = "container.service.pid";

    /**
     * Get the container state
     */
    State getState();

    /**
     * Get the associated host
     */
    HostIdentity getHostIdentity();

    /**
     * Get the parent container
     */
    ContainerIdentity getParentIdentity();

    /**
     * Get the set of child containers
     */
    Set<ContainerIdentity> getChildIdentities();

    /**
     * Get the set of provided management domains
     * [TODO] Should management domains go to {@link JMXServiceEndpoint}?
     */
    Set<String> getManagementDomains();

    /**
     * Get the set of available service endpoints for the given type
     * @param type null for all types
     */
    <T extends ServiceEndpoint> Set<ServiceEndpointIdentity<?>> getEndpointIdentities(Class<T> type);

    /**
     * Get the profile version
     */
    Version getProfileVersion();

    /**
     * Get the associated list of profiles
     */
    Set<String> getProfileIdentities();
}
