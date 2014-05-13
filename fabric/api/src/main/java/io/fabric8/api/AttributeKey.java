/*
 * #%L
 * Fabric8 :: API
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
package io.fabric8.api;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * A typed attribute key
 *
 * The identity of an attribute key is defined by its name
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class AttributeKey<T> {

    /**
     * A factory to create an attribute value
     */
    public interface ValueFactory<T> {

        /**
         * Create the attribute value from the given source
         * @throws IllegalArgumentException if value cannot be create from the given source
         */
        T createFrom(Object source);
    }

    private final Class<T> type;
    private final String name;
    private final ValueFactory<T> factory;
    private final String tostring;

    public static <T> AttributeKey<T> create(Class<T> type) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        return new AttributeKey<T>(type.getName(), type, null);
    }

    public static <T> AttributeKey<T> create(String name, Class<T> type) {
        return new AttributeKey<T>(name, type, null);
    }

    public static <T> AttributeKey<T> create(String name, Class<T> type, ValueFactory<T> factory) {
        return new AttributeKey<T>(name, type, factory);
    }

    private AttributeKey(String name, Class<T> type, ValueFactory<T> factory) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        IllegalArgumentAssertion.assertNotNull(type, "type");
        this.name = name;
        this.type = type;
        this.factory = factory;
        this.tostring = "Key[name=" + name + ",type=" + type.getName() + "]";

    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public ValueFactory<T> getFactory() {
        return factory;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AttributeKey)) return false;
        AttributeKey<?> other = (AttributeKey<?>) obj;
        return other.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return tostring;
    }
}
