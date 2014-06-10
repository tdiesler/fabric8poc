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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A typed attribute key
 *
 * The identity of an attribute key is defined by its name
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class AttributeKey<T> {

    public static final String ATTRIBUTE_FORMAT = "key=%s;type=%s;value=%s";
    public static final String GROUP = "([a-zA-Z0-9\\.\\-]+)";
    public static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(String.format(ATTRIBUTE_FORMAT, GROUP, GROUP, GROUP));

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

    public static final ValueFactory<String> STRING_VALUE_FACTORY = new ValueFactory<String>() {
        @Override
        public String createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof String) {
                return (String) source;
            } else {
                return String.valueOf(source);
            }
        }
    };

    public static final ValueFactory<Short> SHORT_VALUE_FACTORY = new ValueFactory<Short>() {
        @Override
        public Short createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Short) {
                return (Short) source;
            } else if (source instanceof Number) {
                return ((Number)source).shortValue();
            } else {
                return Short.parseShort(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    public static final ValueFactory<Integer> INT_VALUE_FACTORY = new ValueFactory<Integer>() {
        @Override
        public Integer createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Integer) {
                return (Integer) source;
            } else if (source instanceof Number) {
                return ((Number)source).intValue();
            } else {
                return Integer.parseInt(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    public static final ValueFactory<Long> LONG_VALUE_FACTORY = new ValueFactory<Long>() {
        @Override
        public Long createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Long) {
                return (Long) source;
            } else if (source instanceof Number) {
                return ((Number)source).longValue();
            } else {
                return Long.parseLong(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    public static final ValueFactory<Float> FLOAT_VALUE_FACTORY = new ValueFactory<Float>() {
        @Override
        public Float createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Float) {
                return (Float) source;
            } else if (source instanceof Number) {
                return ((Number)source).floatValue();
            } else {
                return Float.parseFloat(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    public static final ValueFactory<Double> DOUBLE_VALUE_FACTORY = new ValueFactory<Double>() {
        @Override
        public Double createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Double) {
                return (Double) source;
            } else if (source instanceof Number) {
                return ((Number)source).doubleValue();
            } else {
                return Double.parseDouble(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    public static final ValueFactory<Boolean> BOOLEAN_VALUE_FACTORY = new ValueFactory<Boolean>() {
        @Override
        public Boolean createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Long) {
                return (Boolean) source;
            } else {
                return Boolean.parseBoolean(STRING_VALUE_FACTORY.createFrom(source));
            }
        }
    };

    private final Class<T> type;
    private final String name;
    private final ValueFactory<T> factory;
    private final String tostring;

    private static final Map<Class, ValueFactory> SUPPORTED_VALUE_FACTORIES = new HashMap<>();

    static {
        SUPPORTED_VALUE_FACTORIES.put(String.class, STRING_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Short.class, SHORT_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Integer.class, INT_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Long.class, LONG_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Double.class, DOUBLE_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Float.class, FLOAT_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Boolean.class, BOOLEAN_VALUE_FACTORY);
    }

    public static <T> AttributeKey<T> create(String str) {
        IllegalArgumentAssertion.assertNotNull(str, "string");
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(str);
        if (matcher.matches()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            try {
                Class<T> clazz = (Class<T>) Class.forName(type);
                return create(name, clazz);
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        } else {
            throw new IllegalArgumentException("String: "+str+" does not match pattern:"+ ATTRIBUTE_PATTERN.toString());
        }
    }

    public static <T> AttributeKey<T> create(Class<T> type) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        return new AttributeKey<T>(type.getName(), type, SUPPORTED_VALUE_FACTORIES.get(type));
    }

    public static <T> AttributeKey<T> create(String name, Class<T> type) {
        return new AttributeKey<T>(name, type, SUPPORTED_VALUE_FACTORIES.get(type));
    }

    private static <T> AttributeKey<T> create(String name, Class<T> type, ValueFactory<T> factory) {
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

    public T parse(String str) {
        IllegalArgumentAssertion.assertNotNull(str, "string");
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(str);
        if (matcher.matches()) {
            String value = matcher.group(3);
            return factory.createFrom(value);
        } else {
            throw new IllegalArgumentException("String: " + str + " does not match pattern:" + ATTRIBUTE_PATTERN.toString());
        }
    }

    public String toString(T value) {
        return String.format(ATTRIBUTE_FORMAT, getName(), getType().getCanonicalName(), value);
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
