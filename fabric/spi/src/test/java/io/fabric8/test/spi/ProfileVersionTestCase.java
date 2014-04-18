/*
 * #%L
 * Gravia :: Runtime :: Embedded
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
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
package io.fabric8.test.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.AttributeKey.Factory;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.spi.DefaultProfileVersionBuilder;
import io.fabric8.spi.management.AttributesOpenType;
import io.fabric8.spi.management.ProfileVersionOpenType;

import javax.management.openmbean.CompositeData;

import org.jboss.gravia.resource.Version;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test the {@link ProfileVersion}.
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 */
public class ProfileVersionTestCase {

    static AttributeKey<String> AKEY = AttributeKey.create("AKey", String.class, new ValueFactory());
    static AttributeKey<String> BKEY = AttributeKey.create("BKey", String.class, new ValueFactory());

    @Test
    public void testComposisteData() throws Exception {

        ProfileVersionBuilder builder = new DefaultProfileVersionBuilder();
        builder.addIdentity(Version.parseVersion("1.0"));
        builder.addAttribute(AKEY, "AVal");
        builder.addAttribute(BKEY, "BVal");
        ProfileVersion pversion = builder.getProfileVersion();

        CompositeData pversionData = ProfileVersionOpenType.getCompositeData(pversion);
        Assert.assertEquals("1.0.0", pversionData.get(ProfileVersionOpenType.ITEM_IDENTITY));
        CompositeData[] attsData = (CompositeData[]) pversionData.get(ProfileVersionOpenType.ITEM_ATTRIBUTES);
        Assert.assertEquals(2, attsData.length);
        Assert.assertEquals("AKey", attsData[0].get(AttributesOpenType.AttributeType.ITEM_KEY));
        Assert.assertEquals("AVal", attsData[0].get(AttributesOpenType.AttributeType.ITEM_VALUE));
        Assert.assertEquals("BKey", attsData[1].get(AttributesOpenType.AttributeType.ITEM_KEY));
        Assert.assertEquals("BVal", attsData[1].get(AttributesOpenType.AttributeType.ITEM_VALUE));

        ProfileVersion result = ProfileVersionOpenType.getProfileVersion(pversionData);
        Assert.assertEquals("1.0.0", result.getIdentity().toString());
        Assert.assertEquals(2, result.getAttributeKeys().size());
        Assert.assertEquals("AVal", result.getAttribute(AKEY).toString());
        Assert.assertEquals("BVal", result.getAttribute(BKEY).toString());
    }

    public static class ValueFactory implements Factory<String> {
        @Override
        public String createFrom(Object source) {
            return (String) source;
        }
    }
}