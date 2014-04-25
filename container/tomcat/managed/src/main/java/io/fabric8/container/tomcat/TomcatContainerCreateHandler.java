/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */
package io.fabric8.container.tomcat;

import io.fabric8.api.CreateOptions;
import io.fabric8.spi.AbstractManagedContainerHandle;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.ContainerHandle;
import io.fabric8.spi.ManagedContainer;

/**
 * The Tomcat container create handler
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class TomcatContainerCreateHandler implements ContainerCreateHandler {

    @Override
    public boolean accept(CreateOptions options) {
        return options instanceof TomcatCreateOptions;
    }

    @Override
    public ContainerHandle create(final CreateOptions options) {
        final TomcatManagedContainer container = new TomcatManagedContainer((TomcatCreateOptions) options);
        container.create();
        return new TomcatContainerHandle(container);
    }

    static class TomcatContainerHandle extends AbstractManagedContainerHandle {

        TomcatContainerHandle(ManagedContainer<?> container) {
            super(container);
        }
    }
}
