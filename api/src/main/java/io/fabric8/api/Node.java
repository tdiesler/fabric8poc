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


/**
 * The abstraction of a Fabric8 node
 *
 * [TODO] node lifecycle?
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Node extends IdentitySupport, AttributeSupport, ProfileSupport, VersionSupport {

    /**
     * Get the set of associated containers
     */
    Set<Container> getContainers();

    /**
     * Get a container with a given identity
     * @return the container or <code>null</code>
     */
    Container getContainerById(Identity identity);

    /**
     * Get the set of management domains provided bu the node
     */
    Set<String> getManagementDomains();

    /**
     * Get the set of available service endpoints
     */
    Set<ServiceEndpoint> getServiceEndpoints();

    /**
     * Ping this node
     */
    boolean ping();
}
