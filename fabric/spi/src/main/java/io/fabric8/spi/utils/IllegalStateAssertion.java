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
package io.fabric8.spi.utils;

/**
 * IllegalState assertions
 *
 * [TODO] Move to Gravia
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class IllegalStateAssertion {

    // hide ctor
    private IllegalStateAssertion() {
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     */
    public static void assertNull(Object value, String message) {
        if (value != null)
            throw new IllegalStateException(message);
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     */
    public static void assertNotNull(Object value, String message) {
        if (value == null)
            throw new IllegalStateException(message);
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     */
    public static void assertTrue(boolean value, String message) {
        if (!value)
            throw new IllegalStateException(message);
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     */
    public static void assertFalse(boolean value, String message) {
        assertTrue(!value, message);
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static void assertEquals(Object exp, Object was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp.equals(was), message);
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static void assertSame(Object exp, Object was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp == was, message);
    }
}
