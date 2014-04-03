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
package io.fabric8.spi.internal;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.CreateOptions;

import java.util.Set;

public final class DefaultContainerBuilder implements ContainerBuilder {

    private final AttributeSupport attributes = new AttributeSupport();
    private String symbolicName;

    @Override
    public ContainerBuilder addIdentity(String symbolicName) {
        this.symbolicName = symbolicName;
        return this;
    }

    @Override
    public CreateOptions getCreateOptions() {
        return new CreateOptions() {

            @Override
            public <T> boolean hasAttribute(AttributeKey<T> key) {
                return attributes.hasAttribute(key);
            }

            @Override
            public Set<AttributeKey<?>> getAttributeKeys() {
                return attributes.getAttributeKeys();
            }

            @Override
            public <T> T getAttribute(AttributeKey<T> key) {
                return attributes.getAttribute(key);
            }

            @Override
            public String getSymbolicName() {
                return symbolicName;
            }
        };
    }
}
