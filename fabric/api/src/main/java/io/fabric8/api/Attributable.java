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

import java.util.Map;
import java.util.Set;


/**
 * Provide attribute support for a construct
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Attributable {

    /**
     * Get the list of attribute keys
     */
    Set<AttributeKey<?>> getAttributeKeys();

    /**
     * Get an attribute value
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * True if the given attribute key exists
     */
    <T> boolean hasAttribute(AttributeKey<T> key);

    /**
     * Get the map of all attributes
     */
    Map<AttributeKey<?>, Object> getAttributes();
}
