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


import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class AttributeKeyTest {

    @Test
    public void testCreateStringAttribute() {
        AttributeKey<String> key = AttributeKey.create("my.key");
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(String.class, key.getType());
        String value = key.getFactory().createFrom("my.value");
        Assert.assertEquals("my.value", value);
        Assert.assertEquals("Key[name=my.key]", key.toString());
        Assert.assertEquals("name=my.key", key.getCanonicalForm());

        key = AttributeKey.create("my.key", String.class);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(String.class, key.getType());
        value = key.getFactory().createFrom("my.value");
        Assert.assertEquals("my.value", value);
        Assert.assertEquals("Key[name=my.key]", key.toString());
        Assert.assertEquals("name=my.key", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=string");
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(String.class, key.getType());
        value = key.getFactory().createFrom("my.value");
        Assert.assertEquals("my.value", value);
        Assert.assertEquals("Key[name=my.key]", key.toString());
        Assert.assertEquals("name=my.key", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key");
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(String.class, key.getType());
        value = key.getFactory().createFrom("my.value");
        Assert.assertEquals("my.value", value);
        Assert.assertEquals("Key[name=my.key]", key.toString());
        Assert.assertEquals("name=my.key", key.getCanonicalForm());
    }

    @Test
    public void testCreateShortAttribute() {
        AttributeKey<Short> key = AttributeKey.createFrom("name=my.key,type=short");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Short.class, key.getType());
        short value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Short]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Short", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Short.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Short.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Short]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Short", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Short");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Short.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Short]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Short", key.getCanonicalForm());
    }

    @Test
    public void testCreateIntegerAttribute() {
        AttributeKey<Integer> key = AttributeKey.createFrom("name=my.key,type=integer");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Integer.class, key.getType());
        int value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Integer]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Integer", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Integer.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Integer.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Integer]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Integer", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=int");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Integer.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Integer]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Integer", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Integer");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Integer.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Integer]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Integer", key.getCanonicalForm());
    }

    @Test
    public void testCreateLongAttribute() {
        AttributeKey<Long> key = AttributeKey.createFrom("name=my.key,type=long");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Long.class, key.getType());
        long value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Long]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Long", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Long.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Long.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Long]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Long", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Long");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Long.class, key.getType());
        value = key.getFactory().createFrom("1");
        Assert.assertEquals(1, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Long]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Long", key.getCanonicalForm());
    }

    @Test
    public void testCreateFloatAttribute() {
        AttributeKey<Float> key = AttributeKey.createFrom("name=my.key,type=float");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Float.class, key.getType());
        float value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Float]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Float", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Float.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Float.class, key.getType());
        value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Float]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Float", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Float");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Float.class, key.getType());
        value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Float]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Float", key.getCanonicalForm());
    }

    @Test
    public void testCreateDoubleAttribute() {
        AttributeKey<Double> key = AttributeKey.createFrom("name=my.key,type=double");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Double.class, key.getType());
        double value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Double]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Double", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Double.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Double.class, key.getType());
        value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Double]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Double", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Double");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Double.class, key.getType());
        value = key.getFactory().createFrom("1.0");
        Assert.assertEquals(1.0, value, 0);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Double]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Double", key.getCanonicalForm());
    }

    @Test
    public void testCreateBooleanAttribute() {
        AttributeKey<Boolean> key = AttributeKey.createFrom("name=my.key,type=boolean");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Boolean.class, key.getType());
        boolean value = key.getFactory().createFrom("true");
        Assert.assertEquals(true, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Boolean]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Boolean", key.getCanonicalForm());

        key = AttributeKey.create("my.key", Boolean.class);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Boolean.class, key.getType());
        value = key.getFactory().createFrom("true");
        Assert.assertEquals(true, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Boolean]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Boolean", key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,type=java.lang.Boolean");
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(Boolean.class, key.getType());
        value = key.getFactory().createFrom("true");
        Assert.assertEquals(true, value);
        Assert.assertEquals("Key[name=my.key,type=java.lang.Boolean]", key.toString());
        Assert.assertEquals("name=my.key,type=java.lang.Boolean", key.getCanonicalForm());
    }


    @Test
    public void testCreateURLAttribute() throws MalformedURLException {
        AttributeKey<URL> key = AttributeKey.create("my.key", new URLValueFactory());
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(URL.class, key.getType());
        URL value = key.getFactory().createFrom("http://foo");
        Assert.assertEquals(new URL("http://foo"), value);
        String factoryName = URLValueFactory.class.getName();
        Assert.assertEquals("Key[name=my.key,type=java.net.URL,factory=" + factoryName + "]", key.toString());
        Assert.assertEquals("name=my.key,type=java.net.URL,factory=" + factoryName, key.getCanonicalForm());

        key = AttributeKey.createFrom("name=my.key,factory=" + factoryName);
        Assert.assertNotNull(key);
        Assert.assertEquals("my.key", key.getName());
        Assert.assertEquals(URL.class, key.getType());
        value = key.getFactory().createFrom("http://foo");
        Assert.assertEquals(new URL("http://foo"), value);
        Assert.assertEquals("Key[name=my.key,type=java.net.URL,factory=" + factoryName + "]", key.toString());
        Assert.assertEquals("name=my.key,type=java.net.URL,factory=" + factoryName, key.getCanonicalForm());
    }
}