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

/**
 * The main entry point to the Fabric8 system.
 *
 * An instance of this service can be obtained from the service registry.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface FabricManager {

    /**
     * Create a container with a given name
     * @throws IllegalStateException if a container with the given name already exists
     */
    Container createContainer(String name);

    /**
     * Get a container with a given name
     * @return the container or <code>null</code>
     */
    Container getContainerByName(String name);
}
