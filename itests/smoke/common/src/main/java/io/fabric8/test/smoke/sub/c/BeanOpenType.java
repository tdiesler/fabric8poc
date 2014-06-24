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
package io.fabric8.test.smoke.sub.c;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * CompositeData support for a {@link Bean}.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class BeanOpenType {

    public static final String TYPE_NAME = "BeanType";
    public static final String ITEM_NAME = "name";
    public static final String ITEM_VALUE = "value";

    // Hide ctor
    private BeanOpenType() {
    }

    private static final CompositeType compositeType;
    static {
        try {
            compositeType = new CompositeType(TYPE_NAME, TYPE_NAME, getItemNames(), getItemNames(), getItemTypes());
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static CompositeType getCompositeType() {
        return compositeType;
    }

    public static CompositeData toCompositeData(Bean bean) {
        List<String> items = new ArrayList<>();
        items.add(bean.getName());
        items.add(bean.getValue());
        try {
            Object[] itemValues = items.toArray(new Object[items.size()]);
            return new CompositeDataSupport(compositeType, getItemNames(), itemValues);
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Bean fromCompositeData(CompositeData cdata) {
        String name = (String) cdata.get(BeanOpenType.ITEM_NAME);
        String value = (String) cdata.get(BeanOpenType.ITEM_VALUE);
        return new Bean(name, value);
    }

    public static String[] getItemNames() {
        return new String[] { ITEM_NAME, ITEM_VALUE };
    }

    public static OpenType<?>[] getItemTypes() throws OpenDataException {
        return new OpenType<?>[] { SimpleType.STRING, SimpleType.STRING };
    }
}
