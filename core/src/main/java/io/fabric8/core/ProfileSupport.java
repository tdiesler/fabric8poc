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
package io.fabric8.core;

import io.fabric8.api.ConfigurationItem;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


final class ProfileSupport {

    // Hide ctor
    private ProfileSupport() {
    }

    static void applyConfigurationItems(ConfigurationAdmin configAdmin, Set<ConfigurationItem> configurationItems) {
        for (ConfigurationItem item : configurationItems) {
            try {
                Configuration config = configAdmin.getConfiguration(item.getIdentity(), null);
                config.update(toDictionary(item.getConfiguration()));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot update configuration: " + item.getIdentity(), ex);
            }
        }
    }

    static private Dictionary<String, String> toDictionary(Map<String, String> configuration) {
        Dictionary<String, String> result = new Hashtable<String, String>();
        for (Entry<String, String> entry : configuration.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
