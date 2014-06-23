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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * A typed attribute key
 *
 * The identity of an attribute key is defined by its name
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class AttributeKey<T> implements Identity {

    public static final String ATTRIBUTE_KEY_FORMAT = "(%s)(,type=(?<type>%s))?(,factory=(?<factory>%s))?";
    public static final Pattern ATTRIBUTE_KEY_PATTERN = Pattern.compile(String.format(ATTRIBUTE_KEY_FORMAT, Identity.GROUP, Identity.GROUP, Identity.GROUP));

    /**
     * A factory to create an attribute value
     */
    public interface ValueFactory<T> {

        /**
         * Get the value type that this factory creates
         */
        Class<T> getType();

        /**
         * Create the attribute value from the given source
         * @throws IllegalArgumentException if value cannot be create from the given source
         */
        T createFrom(Object source);
    }

    private final String name;
    private final Class<T> type;
    private final ValueFactory<T> factory;
    private final String canonicalForm;
    private final String toString;

    /**
     * Create an attribute key with the given name
     */
    public static <T> AttributeKey<T> create(String name) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        return new AttributeKey<>(name, null, null);
    }

    /**
     * Create an attribute key for the given type
     */
    public static <T> AttributeKey<T> create(Class<T> type) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        return new AttributeKey<T>(type.getName(), type, null);
    }

    /**
     * Create an attribute key with the given name and type
     */
    public static <T> AttributeKey<T> create(String name, Class<T> type) {
        return new AttributeKey<T>(name, type, null);
    }

    /**
     * Create an attribute key with the given name and type
     */
    public static <T> AttributeKey<T> create(String name, ValueFactory<T> factory) {
        return new AttributeKey<T>(name, null, factory);
    }

    /**
     * Create an attribute key from its canonical form.
     */
    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> createFrom(String canonical) {
        Matcher matcher = assertCanonicalForm(canonical);
        String name = matcher.group(1);
        String typeName = matcher.group("type");
        String factoryName = matcher.group("factory");
        ValueFactory<T> factory = null;
        if (factoryName != null) {
            try {
                factory = (ValueFactory<T>) Class.forName(factoryName).newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot load factory: " + factoryName);
            }
        }
        Class<T> type = null;
        if (typeName != null) {
            try {
                type = (Class<T>) Class.forName(typeName);
            } catch (ClassNotFoundException ex) {
                type = (Class<T>) SUPPORTED_TYPE_NAMES.get(typeName);
            }
        }
        return new AttributeKey<T>(name, type, factory);
    }

    @SuppressWarnings("unchecked")
    private AttributeKey(String name, Class<T> optionalType, ValueFactory<T> customFactory) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        this.name = name;

        // Assign type
        if (optionalType != null) {
            type = optionalType;
        } else {
            if (customFactory != null) {
                type = customFactory.getType();
            } else {
                type = (Class<T>) String.class;
            }
        }
        IllegalArgumentAssertion.assertNotNull(type, "Cannot obtain value type");

        // Assign factory
        if (customFactory != null) {
            factory = customFactory;
            IllegalArgumentAssertion.assertTrue(type == factory.getType(), "Provided type does not match factory type");
        } else {
            ValueFactory<T> supportedFactory = (ValueFactory<T>) SUPPORTED_VALUE_FACTORIES.get(type);
            factory = supportedFactory != null ? supportedFactory : (ValueFactory<T>) STRING_VALUE_FACTORY;
        }

        // Assign cononical form
        StringBuffer buffer = new StringBuffer(name);
        if (type != String.class) {
            buffer.append(",type=" + type.getName());
        }
        String factoryName = factory.getClass().getName();
        if (!factoryName.startsWith(AttributeKey.class.getName())) {
            buffer.append(",factory=" + factoryName);
        }
        canonicalForm = buffer.toString();
        assertCanonicalForm(canonicalForm);
        toString = "Key[name=" + canonicalForm + "]";
    }

    private static Matcher assertCanonicalForm(String canonical) {
        IllegalArgumentAssertion.assertNotNull(canonical, "canonical");
        Matcher matcher = ATTRIBUTE_KEY_PATTERN.matcher(canonical);
        IllegalArgumentAssertion.assertTrue(matcher.matches(), "Parameter '" + canonical + "'does not match pattern: " + ATTRIBUTE_KEY_PATTERN);
        return matcher;
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
    public String getCanonicalForm() {
        return canonicalForm;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AttributeKey))
            return false;
        AttributeKey<?> other = (AttributeKey<?>) obj;
        return other.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return toString;
    }

    private static final ValueFactory<Boolean> BOOLEAN_VALUE_FACTORY = new AbstractValueFactory<Boolean>(Boolean.class) {
        @Override
        public Boolean createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            return Boolean.parseBoolean(source.toString());
        }
    };

    private static final ValueFactory<Double> DOUBLE_VALUE_FACTORY = new AbstractValueFactory<Double>(Double.class) {
        @Override
        public Double createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Number) {
                return ((Number) source).doubleValue();
            } else {
                return Double.parseDouble(source.toString());
            }
        }
    };

    private static final ValueFactory<Float> FLOAT_VALUE_FACTORY = new AbstractValueFactory<Float>(Float.class) {
        @Override
        public Float createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Number) {
                return ((Number) source).floatValue();
            } else {
                return Float.parseFloat(source.toString());
            }
        }
    };

    private static final ValueFactory<Integer> INTEGER_VALUE_FACTORY = new AbstractValueFactory<Integer>(Integer.class) {
        @Override
        public Integer createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Number) {
                return ((Number) source).intValue();
            } else {
                return Integer.parseInt(source.toString());
            }
        }
    };

    private static final ValueFactory<Long> LONG_VALUE_FACTORY = new AbstractValueFactory<Long>(Long.class) {
        @Override
        public Long createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Number) {
                return ((Number) source).longValue();
            } else {
                return Long.parseLong(source.toString());
            }
        }
    };

    private static final ValueFactory<Short> SHORT_VALUE_FACTORY = new AbstractValueFactory<Short>(Short.class) {
        @Override
        public Short createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            if (source instanceof Number) {
                return ((Number) source).shortValue();
            } else {
                return Short.parseShort(source.toString());
            }
        }
    };

    private static final ValueFactory<String> STRING_VALUE_FACTORY = new AbstractValueFactory<String>(String.class) {
        @Override
        public String createFrom(Object source) {
            IllegalArgumentAssertion.assertNotNull(source, "source");
            return source.toString();
        }
    };

    private static abstract class AbstractValueFactory<T> implements ValueFactory<T> {
        private final Class<T> type;

        AbstractValueFactory(Class<T> type) {
            this.type = type;
        }

        @Override
        public Class<T> getType() {
            return type;
        }
    }

    private static final Map<Class<?>, ValueFactory<?>> SUPPORTED_VALUE_FACTORIES = new HashMap<>();
    static {
        SUPPORTED_VALUE_FACTORIES.put(Boolean.class, BOOLEAN_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Double.class, DOUBLE_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Float.class, FLOAT_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Integer.class, INTEGER_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Long.class, LONG_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(Short.class, SHORT_VALUE_FACTORY);
        SUPPORTED_VALUE_FACTORIES.put(String.class, STRING_VALUE_FACTORY);
    }
    private static final Map<String, Class<?>> SUPPORTED_TYPE_NAMES = new HashMap<>();
    static {
        SUPPORTED_TYPE_NAMES.put("boolean", Boolean.class);
        SUPPORTED_TYPE_NAMES.put("double", Double.class);
        SUPPORTED_TYPE_NAMES.put("float", Float.class);
        SUPPORTED_TYPE_NAMES.put("int", Integer.class);
        SUPPORTED_TYPE_NAMES.put("integer", Integer.class);
        SUPPORTED_TYPE_NAMES.put("long", Long.class);
        SUPPORTED_TYPE_NAMES.put("short", Short.class);
        SUPPORTED_TYPE_NAMES.put("string", String.class);
    }
}
