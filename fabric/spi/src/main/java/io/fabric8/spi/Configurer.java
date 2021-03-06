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
package io.fabric8.spi;

import java.util.Dictionary;
import java.util.Map;

public interface Configurer {

    /**
     * Configures the specified instance with the provided configuration.
     * @param configuration The configuration.
     * @param target        The target that will receive the configuration.
     */
    <T> Map<String, Object> configure(Map<String, Object> configuration, T target, String... ignorePrefix) throws Exception;

    <T> Map<String, Object> configure(Dictionary<String, Object> configuration, T target, String... ignorePrefix) throws Exception;
}
