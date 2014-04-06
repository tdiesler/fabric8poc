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
package io.fabric8.core.spi;

import io.fabric8.core.api.AttributeKey;
import io.fabric8.core.api.ProfileItem;

import java.util.Set;

import org.jboss.gravia.utils.NotNullException;

public abstract class AbstractProfileItem implements ProfileItem {

    private final String identity;

    protected AbstractProfileItem(String identity) {
        NotNullException.assertValue(identity, "identity");
        this.identity = identity;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentity() {
        return identity;
    }
}
