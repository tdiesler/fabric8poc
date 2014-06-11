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

import io.fabric8.api.Configuration;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.spi.DefaultProfileBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A set of profile utils
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Apr-2014
 */
public final class ProfileUtils {

    // Hide ctor
    private ProfileUtils() {
    }

    public static Profile getEffectiveProfile(LinkedProfile profile) {
        String identity = "effective#" + profile.getIdentity();
        ProfileBuilder prfBuilder = new DefaultProfileBuilder(identity);
        buildEffectiveProfile(prfBuilder, profile);
        return prfBuilder.getProfile();
    }

    public static void buildEffectiveProfile(ProfileBuilder builder, LinkedProfile profile) {

        // Add parent content
        for (String identity : profile.getParents()) {
            LinkedProfile parent = profile.getLinkedParent(identity);
            buildEffectiveProfile(builder, parent);
        }

        // Add attributes
        builder.addAttributes(profile.getAttributes());

        // Add profile items
        for (ProfileItem item : profile.getProfileItems(null)) {
            String itemId = item.getIdentity();

            // Merge with existing {@link ConfigurationItem}
            ConfigurationItem targetItem = builder.getProfile().getProfileItem(itemId, ConfigurationItem.class);
            if (item instanceof ConfigurationItem && targetItem != null) {

                builder.removeProfileItem(itemId);

                ConfigurationItem mergedItem = getMergedConfigurationItem(targetItem, (ConfigurationItem) item);
                if (mergedItem != null) {
                    builder.addProfileItem(mergedItem);
                }
            } else {
                // Add unmodified
                builder.addProfileItem(item);
            }
        }
    }

    private static ConfigurationItem getMergedConfigurationItem(ConfigurationItem targetItem, ConfigurationItem thisItem) {
        int configCount = 0;
        String itemId = targetItem.getIdentity();
        ConfigurationItemBuilder itemBuilder = ConfigurationItemBuilder.Factory.create(itemId);
        for (Configuration tagetConfig : targetItem.getConfigurations(null)) {
            String mergeIndex = tagetConfig.getMergeId();
            Map<String, Object> atts = new LinkedHashMap<>(tagetConfig.getAttributes());
            Map<String, String> dirs = new LinkedHashMap<>(tagetConfig.getDirectives());
            Configuration thisConfig = thisItem.getConfiguration(mergeIndex);
            if (thisConfig != null) {
                Map<String, Object> thisAtts = thisConfig.getAttributes();
                Map<String, String> thisDirs = thisConfig.getDirectives();
                if (thisAtts.get(Configuration.DELETED_MARKER) == null) {
                    for (Entry<String, Object> entry : thisAtts.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (Configuration.DELETED_MARKER.equals(value)) {
                            atts.remove(key);
                        } else {
                            atts.put(key, value);
                        }
                    }
                    dirs.putAll(thisDirs);
                } else {
                    atts.clear();
                }
            }
            if (!atts.isEmpty()) {
                itemBuilder.addConfiguration(mergeIndex, atts, dirs);
                configCount++;
            }
        }
        return configCount > 0 ? itemBuilder.getConfigurationItem() : null;
    }
}
