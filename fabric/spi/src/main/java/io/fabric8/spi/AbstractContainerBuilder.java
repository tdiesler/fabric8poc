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

import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.OptionsProvider;

public abstract class AbstractContainerBuilder<B extends ContainerBuilder<B, T>, T extends CreateOptions> extends AbstractAttributableBuilder<B> implements ContainerBuilder<B, T> {

    protected final T options;

    protected AbstractContainerBuilder(T options) {
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B identity(String identity) {
        getMutableOptions().setIdentity(ContainerIdentity.createFrom(identity));
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addOptions(OptionsProvider<B> optionsProvider) {
        return optionsProvider.addBuilderOptions((B) this);
    }

    protected AbstractCreateOptions getMutableOptions() {
        return (AbstractCreateOptions) options;
    }

    @Override
    public T getCreateOptions() {
        getMutableOptions().validate();
        return options;
    }
}
