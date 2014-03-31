/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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


/**
 * Provide attribute support for a construct
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface AttributeSupport {

    /**
     * Get a map of attributes associaed with this container
     */
    Map<String, String> getAttributes();

    /**
     * Get an attribute value for this container
     */
    String getAttribute(String key);

    /**
     * Set an attribute value for this container
     */
    void setAttribute(String key, String value);
}
