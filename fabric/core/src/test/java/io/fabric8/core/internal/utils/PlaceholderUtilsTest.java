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

package io.fabric8.core.internal.utils;

import org.junit.Test;

import io.fabric8.core.utils.PlaceholderUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class PlaceholderUtilsTest {

    @Test
    public void testSubstitution() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("key1", "value1");
        config.put("key2", "value2");
        config.put("key3", "${key2}");
        config.put("key4", "${key5}");
        config.put("key5", "${key4}");


        assertEquals("", PlaceholderUtils.substitute("", config));
        assertEquals("value1", PlaceholderUtils.substitute("${key1}", config));
        assertEquals("value1-value2", PlaceholderUtils.substitute("${key1}-${key2}", config));
        //Nested Substitution
        assertEquals("value1-value2", PlaceholderUtils.substitute("${key1}-${key3}", config));
        //Test infinite loop prevention
        assertEquals("value1-", PlaceholderUtils.substitute("${key1}-${key4}", config));
    }
}
