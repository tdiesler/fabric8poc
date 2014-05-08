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

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.spi.internal.DefaultProfileBuilder;

import java.util.HashMap;
import java.util.Map;

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
        String identity = "effective:" + profile.getIdentity();
        ProfileBuilder prfBuilder = new DefaultProfileBuilder(identity);
        buildEffectiveProfile(prfBuilder, profile);
        return prfBuilder.build();
    }

    private static void buildEffectiveProfile(ProfileBuilder builder, LinkedProfile profile) {

        // Add parent content
        for (String identity : profile.getParents()) {
            LinkedProfile parent = profile.getLinkedParent(identity);
            buildEffectiveProfile(builder, parent);
        }

        // Add attributes
        builder.addAttributes(profile.getAttributes());

        // Add profile items
        for (ProfileItem item : profile.getProfileItems(null)) {

            // Merge with ConfigurationProfileItem
            if (item instanceof ConfigurationItem) {
                String itemId = item.getIdentity();
                ConfigurationItem prevItem = builder.build().getProfileItem(itemId, ConfigurationItem.class);
                if (prevItem != null) {
                    Map<String, Object> config = new HashMap<>(prevItem.getConfiguration());
                    config.putAll(((ConfigurationItem) item).getConfiguration());
                    builder.addConfigurationItem(itemId, config);
                } else {
                    builder.addProfileItem(profile.getProfileItem(itemId, ConfigurationItem.class));
                }
            }
        }
    }
}
