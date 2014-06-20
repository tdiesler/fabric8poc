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
package io.fabric8.spi.management;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ResourceItem;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.AttributeSupport;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.jboss.gravia.resource.ResourceIdentity;

/**
 * CompositeData support for a {@link Profile}.
 *
 * [TODO] #35 Complete JMX functionality on ProfileManagement
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class ProfileOpenType {

    public static final String TYPE_NAME = "ProfileType";
    public static final String ITEM_IDENTITY = "identity";
    public static final String ITEM_ATTRIBUTES = "attributes";

    // Hide ctor
    private ProfileOpenType() {
    }

    private static final CompositeType compositeType;
    static {
        try {
            compositeType = new CompositeType(TYPE_NAME, TYPE_NAME, getItemNames(), getItemNames(), getItemTypes());
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static CompositeType getCompositeType() {
        return compositeType;
    }

    public static CompositeData getCompositeData(Profile profile) {
        ProfileIdentity identity = profile.getIdentity();
        List<Object> items = new ArrayList<Object>();
        items.add(identity.getCanonicalForm());
        items.add(AttributesOpenType.getCompositeData(profile.getAttributes()));
        Object[] itemValues = items.toArray(new Object[items.size()]);
        try {
            return new CompositeDataSupport(compositeType, getItemNames(), itemValues);
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Profile getProfile(CompositeData cdata) {
        return new CompositeDataProfile(cdata, ProfileOpenType.class.getClassLoader());
    }

    public static Profile getProfile(CompositeData cdata, ClassLoader classLoader) {
        return new CompositeDataProfile(cdata, classLoader);
    }

    public static String[] getItemNames() {
        return new String[] { ITEM_IDENTITY, ITEM_ATTRIBUTES };
    }

    public static OpenType<?>[] getItemTypes() throws OpenDataException {
        ArrayType<CompositeType> attsType = AttributesOpenType.getArrayType();
        return new OpenType<?>[] { SimpleType.STRING, attsType };
    }

    static class CompositeDataProfile extends AttributeSupport implements Profile {

        private final ProfileIdentity identity;

        private CompositeDataProfile(CompositeData cdata, ClassLoader classLoader) {
            identity = ProfileIdentity.createFrom((String) cdata.get(ProfileOpenType.ITEM_IDENTITY));
            for (CompositeData attData : (CompositeData[]) cdata.get(ProfileOpenType.ITEM_ATTRIBUTES)) {
                AttributesOpenType.addAttribute(this, attData, classLoader);
            }
            setImmutable(true);
        }

        @Override
        public ProfileIdentity getIdentity() {
            return identity;
        }

        @Override
        public VersionIdentity getVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProfileIdentity> getParents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends ProfileItem> T getProfileItem(String identity, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceItem getProfileItem(ResourceIdentity identity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends ProfileItem> List<T> getProfileItems(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Profile[" + identity + "]";
        }
    }
}
