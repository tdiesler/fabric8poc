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
package io.fabric8.spi.management;

import io.fabric8.api.Attributable;
import io.fabric8.api.AttributeKey;
import io.fabric8.spi.AttributeSupport;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * Open MBean support for an {@link Attributable}.
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class AttributesOpenType {

    public static final String TYPE_NAME = "AttributesType";
    public static final String ITEM_NAME = "attributes";

    private static final ArrayType<CompositeType> arrayType;
    static {
        try {
            arrayType = new ArrayType<CompositeType>(1, getRowType());
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // Hide ctor
    private AttributesOpenType() {
    }

    public static ArrayType<CompositeType> getArrayType() {
        return arrayType;
    }

    public static CompositeData[] getCompositeData(Map<AttributeKey<?>, Object> map) {
        CompositeData[] dataArr = new CompositeData[map.size()];
        String[] itemNames = AttributeType.getItemNames();
        int index = 0;
        for (Entry<AttributeKey<?>, Object> entry : map.entrySet()) {
            AttributeKey<?> key = entry.getKey();
            String name = key.getName();
            String type = key.getType() != null ? key.getType().getName() : null;
            String value = entry.getValue().toString();
            Object[] itemValues = new Object[] { name, value, type };
            try {
                dataArr[index++] = new CompositeDataSupport(AttributeType.getArrayType(), itemNames, itemValues);
            } catch (OpenDataException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return dataArr;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void addAttribute(AttributeSupport attributes, CompositeData attData, ClassLoader classLoader) {
        String name = (String) attData.get(AttributeType.ITEM_KEY);
        String valStr = (String) attData.get(AttributeType.ITEM_VALUE);
        String typeName = (String) attData.get(AttributeType.ITEM_TYPE);
        IllegalStateAssertion.assertNotNull(typeName, "Cannot obtain type name");
        IllegalStateAssertion.assertNotNull(typeName, "Cannot obtain factory name");
        AttributeKey key;
        Class type;
        Object value;
        try {
            type = Class.forName(typeName, true, classLoader);
            key = AttributeKey.create(name, type);
            value = key.getFactory().createFrom(valStr);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        attributes.addAttribute(key, value);
    }

    public static String[] getIndexNames() {
        return new String[] { AttributeType.ITEM_KEY, AttributeType.ITEM_VALUE };
    }

    public static CompositeType getRowType() throws OpenDataException {
        return AttributeType.getArrayType();
    }

    public static final class AttributeType {

        public static final String TYPE_NAME = "AttributeType";
        public static final String ITEM_KEY = "key";
        public static final String ITEM_VALUE = "value";
        public static final String ITEM_TYPE = "type";

        private static final CompositeType compositeType;
        static {
            try {
                compositeType = new CompositeType(TYPE_NAME, TYPE_NAME, getItemNames(), getItemNames(), getItemTypes());
            } catch (OpenDataException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public static CompositeType getArrayType() {
            return compositeType;
        }

        public static String[] getItemNames() {
            return new String[] { ITEM_KEY, ITEM_VALUE, ITEM_TYPE };
        }

        public static OpenType<?>[] getItemTypes() {
            return new OpenType<?>[] { SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};
        }
    }
}

