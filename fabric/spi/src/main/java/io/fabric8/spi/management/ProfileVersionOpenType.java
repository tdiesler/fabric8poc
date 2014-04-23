/*
 * #%L
 * Gravia :: Resource
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
package io.fabric8.spi.management;

import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.AttributeSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.jboss.gravia.resource.Version;

/**
 * CompositeData support for a {@link ProfileVersion}.
 *
 * [TODO] Complete ProfileVersionOpenType
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class ProfileVersionOpenType {

    public static final String TYPE_NAME = "ProfileVersionType";
    public static final String ITEM_IDENTITY = "identity";
    public static final String ITEM_ATTRIBUTES = "attributes";

    // Hide ctor
    private ProfileVersionOpenType() {
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

    public static CompositeData getCompositeData(ProfileVersion pversion) {
        String identity = pversion.getIdentity().toString();
        List<Object> items = new ArrayList<Object>();
        items.add(identity);
        items.add(AttributesOpenType.getCompositeData(pversion.getAttributes()));
        Object[] itemValues = items.toArray(new Object[items.size()]);
        try {
            return new CompositeDataSupport(compositeType, getItemNames(), itemValues);
        } catch (OpenDataException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static ProfileVersion getProfileVersion(CompositeData cdata) {
        return new CompositeDataProfileVersion(cdata, ProfileVersionOpenType.class.getClassLoader());
    }

    public static ProfileVersion getProfileVersion(CompositeData cdata, ClassLoader classLoader) {
        return new CompositeDataProfileVersion(cdata, classLoader);
    }

    public static String[] getItemNames() {
        return new String[] { ITEM_IDENTITY, ITEM_ATTRIBUTES };
    }

    public static OpenType<?>[] getItemTypes() throws OpenDataException {
        ArrayType<CompositeType> attsType = AttributesOpenType.getArrayType();
        return new OpenType<?>[] { SimpleType.STRING, attsType };
    }

    static class CompositeDataProfileVersion extends AttributeSupport implements ProfileVersion {

        private final Version identity;

        private CompositeDataProfileVersion(CompositeData cdata, ClassLoader classLoader) {
            identity = Version.parseVersion((String) cdata.get(ProfileVersionOpenType.ITEM_IDENTITY));
            for (CompositeData attData : (CompositeData[]) cdata.get(ProfileVersionOpenType.ITEM_ATTRIBUTES)) {
                AttributesOpenType.addAttribute(this, attData, classLoader);
            }
        }

        @Override
        public Version getIdentity() {
            return identity;
        }

        @Override
        public Set<ProfileIdentity> getProfileIdentities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "ProfileVersion[" + identity + "]";
        }
    }
}
