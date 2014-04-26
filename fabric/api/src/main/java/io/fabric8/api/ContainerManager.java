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

import java.util.List;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Runtime;

/**
 * A container manager
 *
 * An instance of this service can be obtained from the gravia {@link Runtime}.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerManager {

    /**
     * Aquire an exclusive write lock for the given container
     */
    LockHandle aquireContainerLock(String identity);

    /**
     * Create a container with the given options
     */
    Container createContainer(CreateOptions options);

    /**
     * Create a container with the given options
     */
    Container createContainer(String parentId, CreateOptions options);

    /**
     * Get the set of container identities in the cluster
     */
    Set<String> getContainerIdentities();

    /**
     * Get the set of containers for the given identities
     * @param identities The requested identities or <code>null</code> for all containers
     */
    Set<Container> getContainers(Set<String> identities);

    /**
     * Get the container with the given identity
     */
    Container getContainer(String identity);

    /**
     * Get the current container
     */
    Container getCurrentContainer();

    /**
     * Start the container with the given identity
     */
    Container startContainer(String identity, ProvisionEventListener listener);

    /**
     * Stop the container with the given identity
     */
    Container stopContainer(String identity);

    /**
     * Destroy the container with the given identity
     */
    Container destroyContainer(String identity);

    /**
     * Ping the container with the given identity
     */
    boolean pingContainer(String identity);

    /**
     * Join fabric for the container with the given identity
     * [TODO] How does join relate to states?
     * [TODO] Do we need a listener for join?
     */
    Container joinFabric(String identity, JoinOptions options);

    /**
     * Leave fabric for the container with the given identity
     * [TODO] Do we need a listener for leave?
     */
    Container leaveFabric(String identity);

    /**
     * Set the profile version for the container with the given identity
     */
    Container setProfileVersion(String identity, Version version, ProvisionEventListener listener);

    /**
     * Add profiles to the container with the given identity
     */
    Container addProfiles(String identity, Set<String> profiles, ProvisionEventListener listener);

    /**
     * Remove profiles from the container with the given identity
     */
    Container removeProfiles(String identity, Set<String> profiles, ProvisionEventListener listener);

    /**
     * Get the a service endpoint for the given type
     * @return null if the endpoint does not exist or is not unique
     */
    <T extends ServiceEndpoint> T getServiceEndpoint(String identity, Class<T> type);

    /**
     * Get the service endpoint for the given identity
     * @return null if the endpoint does not exist
     */
    ServiceEndpoint getServiceEndpoint(String identity, ServiceEndpointIdentity<?> endpointId);

    /**
     * Get failures from the container with the given identity
     */
    List<Failure> getFailures(String identity);

    /**
     * Clear failures from the container with the given identity
     */
    List<Failure> clearFailures(String identity);
}
