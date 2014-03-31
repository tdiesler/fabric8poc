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
     * Get the set of nodes in the cluster
     */
    Set<Node> getNodes();

    /**
     * Get the current node
     */
    Node getCurrentNode();

    /**
     * Set the current node
     */
    void setCurrentNode(Node node);

    /**
     * Get a new node builder
     */
    NodeBuilder newNodeBuilder();

    /**
     * Get a new container builder for the current node
     */
    ContainerBuilder newContainerBuilder();

    /**
     * Get a new profile builder for the current node
     */
    ProcessBuilder newProfileBuilder();

    /**
     * Get a new version builder for the current node
     */
    VersionBuilder newVersionBuilder();
}
