/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.karaf;

import io.fabric8.spi.AbstractManagedContainerBuilder;
import io.fabric8.spi.ManagedContainer;



/**
 * The managed container configuration builder
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Apr-2014
 */
public final class KarafContainerBuilder extends AbstractManagedContainerBuilder<KarafContainerBuilder, KarafCreateOptions> {

    public static KarafContainerBuilder create() {
        return new KarafContainerBuilder();
    }

    private KarafContainerBuilder() {
        super(new KarafCreateOptions());
    }

    @Override
    public ManagedContainer<KarafCreateOptions> getManagedContainer() {
        return new KarafManagedContainer(getCreateOptions());
    }
}
