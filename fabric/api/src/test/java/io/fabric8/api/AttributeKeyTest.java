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

package io.fabric8.api;


import org.junit.Assert;
import org.junit.Test;

public class AttributeKeyTest {

    @Test
    public void testCreateStringAttribute() {
        String str = "key=my.key;type=java.lang.String;value=my.value";
        AttributeKey<String> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(String.class, key.getType());
        String value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertEquals("my.value", value);
        Assert.assertEquals(str, key.toString(value));
    }

    @Test
    public void testCreateShortAttribute() {
        String str = "key=my.key;type=java.lang.Short;value=1";
        AttributeKey<Short> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Short.class, key.getType());
        short value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertEquals(1, value);
        Assert.assertEquals(str, key.toString(value));
    }

    @Test
    public void testCreateIntAttribute() {
        String str = "key=my.key;type=java.lang.Integer;value=1";
        AttributeKey<Integer> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Integer.class, key.getType());
        int value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertEquals(1, value);
        Assert.assertEquals(str, key.toString(value));
    }

    @Test
    public void testCreateLongAttribute() {
        String str = "key=my.key;type=java.lang.Long;value=1";
        AttributeKey<Long> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Long.class, key.getType());
        long value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertEquals(1, value);
        Assert.assertEquals(str, key.toString(value));
    }

    @Test
    public void testCreateFloatAttribute() {
        String str = "key=my.key;type=java.lang.Float;value=1.0";
        AttributeKey<Float> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Float.class, key.getType());
        float value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals(str, key.toString(value));
    }

    @Test
    public void testCreateBooleanAttribute() {
        String str = "key=my.key;type=java.lang.Boolean;value=true";
        AttributeKey<Boolean> key = AttributeKey.create(str);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Boolean.class, key.getType());
        boolean value = key.parse(str);
        Assert.assertNotNull(value);
        Assert.assertTrue(value);
        Assert.assertEquals(str, key.toString(value));
    }
}