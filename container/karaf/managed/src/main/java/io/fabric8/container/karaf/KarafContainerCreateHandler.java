/*
 * #%L
 * Fabric8 :: Container :: Karaf :: Managed
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

package io.fabric8.container.karaf;

import io.fabric8.api.CreateOptions;
import io.fabric8.spi.AbstractManagedContainerHandle;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.ContainerHandle;
import io.fabric8.spi.ManagedContainer;

/**
 * The Karaf container create handler
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Apr-2014
 */
public final class KarafContainerCreateHandler implements ContainerCreateHandler {

    @Override
    public boolean accept(CreateOptions options) {
        return options instanceof KarafCreateOptions;
    }

    @Override
    public ContainerHandle create(final CreateOptions options) {
        ManagedContainer<?> container = new KarafManagedContainer((KarafCreateOptions) options);
        container.create();
        return new KarafContainerHandle(container);
    }

    static class KarafContainerHandle extends AbstractManagedContainerHandle {
        KarafContainerHandle(ManagedContainer<?> container) {
            super(container);
        }
    }
}
