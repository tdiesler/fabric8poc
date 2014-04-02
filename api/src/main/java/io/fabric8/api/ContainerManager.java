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
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerManager {

    /**
     * Get a container builder for the given type
     */
    <T extends ContainerBuilder> T getContainerBuilder(Class<T> type);

    /**
     * Create a container with the given options
     */
    Container createContainer(CreateOptions options);

    /**
     * Get the set of containers in the cluster
     */
    Set<ContainerIdentity> getAllContainers();

    /**
     * Get the current container
     */
    Container getCurrentContainer();

    /**
     * Get the container with the given identity
     */
    Container getContainer(ContainerIdentity identity);

    /**
     * Start the container with the given identity
     */
    Container start(ContainerIdentity identity);

    /**
     * Stop the container with the given identity
     */
    Container stop(ContainerIdentity identity);

    /**
     * Destroy the container with the given identity
     */
    Container destroy(ContainerIdentity identity);

    /**
     * Set the version for the container with the given identity
     */
    void setVersion(ContainerIdentity identity, Version version, ProvisionListener listener);

    /**
     * Ping the container with the given identity
     */
    boolean ping(ContainerIdentity identity);

    /**
     * Join fabric for the container with the given identity
     * [TODO]
     * <ol>
     * <li> How does this relate to states?
     * <li> Does this need to be queried?
     * </ol>
     */
    void joinFabric(ContainerIdentity identity, JoinOptions options);

    /**
     * Leave fabric for the container with the given identity
     */
    void leaveFabric(ContainerIdentity identity);

    /**
     * Add profiles to the container with the given identity
     */
    void addProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionListener listener);

    /**
     * Remove profiles from the container with the given identity
     */
    void removeProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionListener listener);

    /**
     * Get failures from the container with the given identity
     */
    List<Failure> getFailures(ContainerIdentity identity);

    /**
     * Clear failures from the container with the given identity
     */
    List<Failure> clearFailures(ContainerIdentity identity);
}
