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

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.utils.NotNullException;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.CreateOptions;

public abstract class AbstractCreateOptions implements CreateOptions {

    private final AttributeSupport attributes = new AttributeSupport();
    private String symbolicName;
    private boolean immutable;

    @Override
    public String getSymbolicName() {
        return symbolicName;
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
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    protected void validateConfiguration() {
        NotNullException.assertValue(symbolicName, "symbolicName");
    }

    // Setters are package protected

    void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    <T> void putAttribute(AttributeKey<T> key, T value) {
        attributes.putAttribute(key, value);
    }

    void makeImmutable() {
        assertMutable();
        immutable = true;
    }

    protected void assertMutable() {
        if (immutable)
            throw new IllegalStateException("Configuration is immutable");
    }
}