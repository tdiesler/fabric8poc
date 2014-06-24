/*
 * #%L
 * Fabric8 :: SPI
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
package io.fabric8.spi;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;

import java.util.Collection;
import java.util.Map;

import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.resource.ResourceIdentity;

/**
 * The current container
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Jun-2014
 */
public interface CurrentContainer {

    ContainerIdentity getCurrentContainerIdentity();

    Container getCurrentContainer();

    Map<ResourceIdentity, ResourceHandle> getResourceHandles();

    void addResourceHandles(Map<ResourceIdentity, ResourceHandle> handles);

    void removeResourceHandles(Collection<ResourceIdentity> handles);
}