/*
 * #%L
 * Gravia :: Resource
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

import io.fabric8.api.Attributable;
import io.fabric8.api.AttributeKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link Attributable}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Apr-2014
 */
public class AttributeSupport implements Attributable {

    private Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<AttributeKey<?>, Object>();

    public AttributeSupport() {
    }

    public AttributeSupport(Map<AttributeKey<?>, Object> initial) {
        attributes.putAll(initial);
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
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.containsKey(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void putAllAttributes(Map<AttributeKey<?>, Object> atts) {
        attributes.putAll(atts);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttribute(AttributeKey<T> key, T value) {
        return (T) attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(AttributeKey<T> key) {
        return (T) attributes.remove(key);
    }
}
