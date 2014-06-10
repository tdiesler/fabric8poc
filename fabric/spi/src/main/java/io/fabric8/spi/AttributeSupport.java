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

import io.fabric8.api.Attributable;
import io.fabric8.api.AttributeKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * An implementation of {@link Attributable}.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Apr-2014
 */
public class AttributeSupport implements Attributable {

    private final Map<AttributeKey<?>, Object> attributes = Collections.synchronizedMap(new LinkedHashMap<AttributeKey<?>, Object>());
    private final AtomicBoolean immutable = new AtomicBoolean();

    public AttributeSupport() {
    }

    public AttributeSupport(Map<AttributeKey<?>, Object> initial, boolean immutable) {
        this.attributes.putAll(initial);
        this.immutable.set(immutable);
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key) {
        return (T) attributes.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRequiredAttribute(AttributeKey<T> key) {
        T value = (T) attributes.get(key);
        IllegalStateAssertion.assertNotNull(value, "Cannot obtain attribute: " + key);
        return value;
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.containsKey(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void addAttributes(Map<AttributeKey<?>, Object> atts) {
        assertMutable();
        attributes.putAll(atts);
    }

    @SuppressWarnings("unchecked")
    public <T> T addAttribute(AttributeKey<T> key, T value) {
        return (T) attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(AttributeKey<T> key) {
        return (T) attributes.remove(key);
    }

    public boolean isImmutable() {
        return immutable.get();
    }

    public void setImmutable(boolean immutable) {
        this.immutable.set(immutable);
    }

    private void assertMutable() {
        IllegalStateAssertion.assertFalse(immutable.get(), "Attributes are not mutable");
    }
}
