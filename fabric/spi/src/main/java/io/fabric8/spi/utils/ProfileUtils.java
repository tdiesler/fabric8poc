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

import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;

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

    public static Profile getEffectiveProfile(LinkedProfile linkedProfile) {
        return getEffectiveProfile(linkedProfile, getLinkedProfiles(linkedProfile, null));
    }

    public static Profile getEffectiveProfile(Profile profile, Map<String, Profile> linkedProfiles) {
        String identity = "effective:" + profile.getIdentity();
        ProfileBuilder prfBuilder = ProfileBuilder.Factory.create(identity);
        addProfileContent(prfBuilder, profile, linkedProfiles);
        return prfBuilder.build();
    }

    private static void addProfileContent(ProfileBuilder prfBuilder, Profile profile, Map<String, Profile> linkedProfiles) {

        // Add parent content
        for (String parentIdentity : profile.getParents()) {
            Profile parentProfile = linkedProfiles.get(parentIdentity);
            addProfileContent(prfBuilder, parentProfile, linkedProfiles);
        }

        // Add attributes
        prfBuilder.addAttributes(profile.getAttributes());

        // Add profile items
        for (ProfileItem item : profile.getProfileItems(null)) {

            // Merge with ConfigurationProfileItem
            if (item instanceof ConfigurationProfileItem) {
                String itemId = item.getIdentity();
                ConfigurationProfileItem prevItem = prfBuilder.build().getProfileItem(itemId, ConfigurationProfileItem.class);
                if (prevItem != null) {
                    Map<String, Object> config = new HashMap<>(prevItem.getConfiguration());
                    config.putAll(((ConfigurationProfileItem) item).getConfiguration());
                    ConfigurationProfileItemBuilder itemBuilder = prfBuilder.getProfileItemBuilder(itemId, ConfigurationProfileItemBuilder.class);
                    item = itemBuilder.setConfiguration(config).build();
                }
            }

            prfBuilder.addProfileItem(item);
        }
    }

    private static Map<String, Profile> getLinkedProfiles(LinkedProfile linkedProfile, Map<String, Profile> linkedProfiles) {
        if (linkedProfiles == null) {
            linkedProfiles = new HashMap<>();
        }
        linkedProfiles.put(linkedProfile.getIdentity(), linkedProfile);
        for (LinkedProfile linkedParent : linkedProfile.getLinkedParents().values()) {
            getLinkedProfiles(linkedParent, linkedProfiles);
        }
        return linkedProfiles;
    }
}
