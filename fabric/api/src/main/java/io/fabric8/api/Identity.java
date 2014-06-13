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

/**
 * An identity
 *
 * @author thomas.diesler@jboss.com
 * @since 13-Jun-2014
 */
public interface Identity {

    public static final String GROUP = "[a-zA-Z0-9\\.\\-\\#\\:]+";

    /**
     * Get the canonical string representation.
     */
    String getCanonicalForm();
}