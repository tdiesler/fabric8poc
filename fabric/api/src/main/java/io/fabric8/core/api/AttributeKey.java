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
package io.fabric8.core.api;



/**
 * A typed attribute key
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class AttributeKey<T> {

    private final Class<T> type;

    public static <T> AttributeKey<T> create(Class<T> type) {
        return new AttributeKey<T>(type);
    }

    private AttributeKey(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }
}
