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

import org.jboss.gravia.runtime.Runtime;

/**
 * The main entry point to the Fabric8 system.
 *
 * An instance of this service can be obtained from the gravia {@link Runtime}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface FabricManager {

    /**
     * Get the set of hosts in the cluster
     */
    Set<Host> getHosts();

    /**
     * Get the current host
     */
    Host getCurrentHost();

    /**
     * Get the set of containers in the cluster
     */
    Set<Container> getContainers();

    /**
     * Get the current container
     */
    Container getCurrentContainer();

    /**
     * Get the set of versions in the cluster
     */
    Set<Version> getVersions();

    /**
     * Get the set of profiles in the cluster
     */
    Set<Profile> getProfiles();
}
