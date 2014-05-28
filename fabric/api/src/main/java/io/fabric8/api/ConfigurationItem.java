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

import java.util.List;
import java.util.Map;

/**
 * A configuration profile item
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ConfigurationItem extends ProfileItem {

    /**
     * A configuration filter
     */
    interface Filter {

        boolean accept(Configuration config);
    }

    /**
     * Get the default config attributes (i.e. first in the list)
     */
    Map<String, Object> getDefaultAttributes();

    /**
     * Get the configuration for the given merge index
     */
    Configuration getConfiguration(String mergeIndex);

    /**
     * Get the list of available configurations
     *
     * @param an optional filter
     */
    List<Configuration> getConfigurations(Filter filter);
}
