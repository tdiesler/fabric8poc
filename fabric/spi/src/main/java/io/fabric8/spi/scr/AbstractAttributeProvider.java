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

package io.fabric8.spi.scr;

import io.fabric8.api.AttributeKey;
import io.fabric8.spi.AttributeListener;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.AttributeSupport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An abstract component for {@link AttributeProvider}.
 */
public abstract class AbstractAttributeProvider extends AbstractComponent implements AttributeProvider {

    private final AttributeSupport delegate = new AttributeSupport();
    private final List<AttributeListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(AttributeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(AttributeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return delegate.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return delegate.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return delegate.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @SuppressWarnings("unchecked")
    public void putAllAttributes(Map<AttributeKey<?>, Object> atts) {
        for (Map.Entry<AttributeKey<?>, Object> entry : atts.entrySet()) {
            putAttribute((AttributeKey<Object>) entry.getKey(), entry.getValue());
        }
    }

    public <T> T putAttribute(AttributeKey<T> key, T value) {
        T oldValue = delegate.putAttribute(key, value);
        for (AttributeListener listener : listeners) {
            if (oldValue == null) {
                listener.attributeAdded(key, value);
            } else {
                listener.attributeChanged(key, value);
            }
        }
        return oldValue;
    }

    public <T> T removeAttribute(AttributeKey<T> key) {
        T value = delegate.removeAttribute(key);
        for (AttributeListener listener : listeners) {
            listener.attributeRemoved(key, value);
        }
        return value;
    }
}
