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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.OptionsProvider;

import java.util.List;
import java.util.Map;

import org.jboss.gravia.resource.Version;

public abstract class AbstractContainerBuilder<B extends ContainerBuilder<B, T>, T extends MutableCreateOptions> implements ContainerBuilder<B, T> {

    protected final T options;

    protected AbstractContainerBuilder(T options) {
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B identity(String identity) {
        options.setIdentity(ContainerIdentity.createFrom(identity));
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B profileVersion(Version version) {
        options.setVersion(version);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B profiles(List<String> profiles) {
        options.setProfiles(profiles);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addOptions(OptionsProvider<B> optionsProvider) {
        return optionsProvider.addBuilderOptions((B) this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> B addAttribute(AttributeKey<V> key, V value) {
        options.addAttribute(key, value);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addAttributes(Map<AttributeKey<?>, Object> atts) {
        options.addAttributes(atts);
        return (B) this;
    }

    @Override
    public T getCreateOptions() {
        options.validate();
        return options;
    }
}
