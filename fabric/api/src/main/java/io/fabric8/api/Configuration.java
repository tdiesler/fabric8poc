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



/**
 * The content of a configuration profile item
 *
 * @author thomas.diesler@jboss.com
 * @since 27-May-2014
 */
public interface Configuration {

    String DEFAULT_MERGE_INDEX = "default";
    String DELETED_MARKER = "#deleted#";

    String getMergeIndex();

    Map<String, Object> getAttributes();

    Object getAttribute(String key);

    Map<String, String> getDirectives();

    String getDirective(String key);
}
