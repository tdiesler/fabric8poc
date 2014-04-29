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
package io.fabric8.spi;

import io.fabric8.api.AttributableBuilder;
import io.fabric8.api.AttributeKey;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Map;

public abstract class AbstractAttributableBuilder<B extends AttributableBuilder<B>> implements AttributableBuilder<B> {

    private final AttributeSupport attributes = new AttributeSupport();
    private boolean immutable;

    @Override
    @SuppressWarnings("unchecked")
    public <V> B addAttribute(AttributeKey<V> key, V value) {
        assertMutable();
        attributes.putAttribute(key, value);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addAttributes(Map<AttributeKey<?>, Object> atts) {
        assertMutable();
        attributes.putAllAttributes(atts);
        return (B) this;
    }

    protected Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    protected void assertMutable() {
        IllegalStateAssertion.assertFalse(immutable, "Builder is immutable");
    }

    protected void makeImmutable() {
        assertMutable();
        immutable = true;
    }
}
