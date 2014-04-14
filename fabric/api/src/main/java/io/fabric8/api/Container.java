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
 * A fabric container
 *
 * @author Thomas.Diesler@jboss.com
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
     * A config key that is made available as a container attribute
     */
    String CNFKEY_CONFIG_TOKEN = "config.token";

    /**
     * An attribute key for the {@link CNFKEY_CONFIG_TOKEN} value
     */
    AttributeKey<String> ATTKEY_CONFIG_TOKEN = AttributeKey.create(String.class);

    /**
     * The configuration PID for this service
     */
    String CONTAINER_SERVICE_PID = "container.service.pid";

    /**
     * Get the associated host
     */
    HostIdentity getHost();

    /**
     * Get the container state
     */
    State getState();

    /**
     * Get the parent container
     */
    ContainerIdentity getParent();

    /**
     * Get the set of child containers
     */
    Set<ContainerIdentity> getChildContainers();

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
    Version getProfileVersion();

    /**
     * Get the associated list of profiles
     */
    Set<ProfileIdentity> getProfiles();
}
