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
package io.fabric8.core.api.management;

import io.fabric8.core.api.Constants;
import io.fabric8.core.api.Container;
import io.fabric8.core.api.ContainerIdentity;

import java.util.Set;

import javax.management.ObjectName;

import org.jboss.gravia.utils.ObjectNameFactory;

/**
 * The container management interface
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Apr-2014
 */
public interface ContainerManagement {

    /**
     * The ObjectName: fabric8:type=ContainerManagement
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create(Constants.MANAGEMENT_DOMAIN + ":type=" + ContainerManagement.class.getSimpleName());

    /**
     * Get the set of container identities in the cluster
     */
    Set<ContainerIdentity> getContainerIds();

    /**
     * Get the container with the given identity
     */
    Container getContainer(ContainerIdentity identity);
}
