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


/**
 * The abstraction of a Fabric8 container
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Container extends IdentitySupport, AttributeSupport, ProfileSupport, VersionSupport, FailureSupport {

    enum State {
        CREATED, STARTED, STOPPED, DESTROYED
    }

    /**
     * Get the current state for this container
     */
    State getState();

    /**
     * Get the node that this container runs on
     */
    Node getNode();

    /**
     * Get the list of child containers
     */
    List<Container> getChildren();

    /**
     * Join an existing fabric
     * [TODO]
     * <ol>
     * <li> How does this relate to states?
     * <li> Does this need to be queried?
     * </ol>
     */
    void joinFabric();

    /**
     * Leave the fabric
     */
    void leaveFabric();

    /**
     * Start this container
     */
    void start();

    /**
     * Stop this container
     */
    void stop();

    /**
     * Destroy this container
     */
    void destroy();

}
