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
package io.fabric8.test.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.spi.DefaultConfigurationItemBuilder;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.DefaultProfileVersionBuilder;
import io.fabric8.spi.DefaultProfileXMLReader;
import io.fabric8.spi.DefaultProfileXMLWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jboss.gravia.repository.RepositoryWriter;
import org.jboss.gravia.repository.spi.AbstractContentHandler;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.IdentityRequirementBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the {@link DefaultProfileXMLWriter} and {@link DefaultProfileXMLReader}.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2014
 */
public class ProfilesReadWriteTest {

    static AttributeKey<String> KEYA = AttributeKey.create("keyA");
    static String RESOURCE_B = "resitemB";

    @Test
    public void testProfileAttributes() throws Exception {

        Profile prfA = new DefaultProfileBuilder("prfA")
            .addAttribute(KEYA, "valA")
            .addAttribute(KEYA, "valB")
            .getProfile();

        Profile prfB = new DefaultProfileBuilder("prfB")
            .addAttribute(KEYA, "valC")
            .addAttribute(KEYA, "valD")
            .getProfile();

        Version version = Version.parseVersion("1.0");
        LinkedProfileVersion exp = new DefaultProfileVersionBuilder(version)
            .addProfile(prfA)
            .addProfile(prfB)
            .getProfileVersion();

        String xmlString = writeProfileVersion(exp, null);
        assertEquals(exp, readProfileVersion(xmlString));
    }

    @Test
    public void testConfigurationItem() throws Exception {

        ConfigurationItem itemA = new DefaultConfigurationItemBuilder("itemA")
        .addConfiguration("midA", Collections.singletonMap("attA", (Object) "valA"), Collections.singletonMap("dirX", "valX"))
        .addConfiguration("midB", Collections.singletonMap("attB", (Object) "valB"), Collections.singletonMap("dirY", "valY"))
        .getConfigurationItem();

        ConfigurationItem itemB = new DefaultConfigurationItemBuilder("itemB")
            .addConfiguration("midA", Collections.singletonMap("attA", (Object) "valA"), Collections.singletonMap("dirX", "valX"))
            .addConfiguration("midB", Collections.singletonMap("attB", (Object) "valB"), Collections.singletonMap("dirY", "valY"))
        .getConfigurationItem();

        Profile prfA = new DefaultProfileBuilder("prfA")
            .addProfileItem(itemA)
            .addProfileItem(itemB)
            .getProfile();

        Version version = Version.parseVersion("1.0");
        LinkedProfileVersion exp = new DefaultProfileVersionBuilder(version)
            .addProfile(prfA)
            .getProfileVersion();

        String xmlString = writeProfileVersion(exp, null);
        assertEquals(exp, readProfileVersion(xmlString));
    }

    @Test
    public void testStreamResource() throws Exception {

        ResourceBuilder resBuilder = new DefaultResourceBuilder();
        resBuilder.addIdentityCapability(ResourceIdentity.fromString(RESOURCE_B));
        resBuilder.addContentCapability(getResourceB().as(ZipExporter.class).exportAsInputStream());
        Resource resA = resBuilder.getResource();

        Profile prfA = new DefaultProfileBuilder("prfA")
            .addResourceItem(resA)
            .getProfile();

        Version version = Version.parseVersion("1.0");
        LinkedProfileVersion exp = new DefaultProfileVersionBuilder(version)
            .addProfile(prfA)
            .getProfileVersion();

        String xmlString = writeProfileVersion(exp, new TestContentHandler());
        assertEquals(exp, readProfileVersion(xmlString));
    }

    @Test
    public void testRequirementItem() throws Exception {

        ResourceIdentity featureId = ResourceIdentity.fromString("camel.core.feature:0.0.0");
        Requirement requirement = new IdentityRequirementBuilder(featureId).getRequirement();

        Profile prfA = new DefaultProfileBuilder("prfA")
            .addRequirementItem(requirement)
            .getProfile();

        Version version = Version.parseVersion("1.0");
        LinkedProfileVersion exp = new DefaultProfileVersionBuilder(version)
            .addProfile(prfA)
            .getProfileVersion();

        String xmlString = writeProfileVersion(exp, new TestContentHandler());
        assertEquals(exp, readProfileVersion(xmlString));
    }

    @Test
    public void testParentProfiles() throws Exception {

        Profile prfA = new DefaultProfileBuilder("prfA")
            .getProfile();

        Profile prfB = new DefaultProfileBuilder("prfB")
            .addParentProfile(prfA.getIdentity())
            .getProfile();

        Version version = Version.parseVersion("1.0");
        LinkedProfileVersion exp = new DefaultProfileVersionBuilder(version)
            .addProfile(prfA)
            .addProfile(prfB)
            .getProfileVersion();

        String xmlString = writeProfileVersion(exp, null);
        assertEquals(exp, readProfileVersion(xmlString));
    }

    private String writeProfileVersion(LinkedProfileVersion profileVersion, RepositoryWriter.ContentHandler contentHandler) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DefaultProfileXMLWriter writer = new DefaultProfileXMLWriter(baos, contentHandler);
        writer.writeProfileVersion(profileVersion);
        writer.close();
        String xmlString = new String(baos.toByteArray());
        System.out.println(pretty(xmlString));
        return xmlString;
    }

    private LinkedProfileVersion readProfileVersion(String xmlString) {
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlString.getBytes());
        DefaultProfileXMLReader reader = new DefaultProfileXMLReader(bais);
        Version version = reader.getProfileVersion();
        ProfileVersionBuilder builder = new DefaultProfileVersionBuilder(version);
        Profile profile = reader.nextProfile();
        while (profile != null) {
            builder.addProfile(profile);
            profile = reader.nextProfile();
        }
        return builder.getProfileVersion();
    }

    private String pretty(String xmlString) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlString.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new StreamSource(bais), new StreamResult(baos));
        return new String(baos.toByteArray());
    }

    private void assertEquals(LinkedProfileVersion exp, LinkedProfileVersion was) throws Exception {
        //writeProfileVersion(was);
        Assert.assertEquals(exp.getIdentity(), was.getIdentity());
        Assert.assertEquals(exp.getLinkedProfiles(), was.getLinkedProfiles());
        for (Profile pwas : was.getLinkedProfiles().values()) {
            Profile pexp = exp.getLinkedProfile(pwas.getIdentity());
            Assert.assertEquals(pexp.getAttributes(), pwas.getAttributes());
            Assert.assertEquals(pexp.getParents(), pwas.getParents());
            Assert.assertEquals(pexp.getProfileItems(null), pwas.getProfileItems(null));
        }
    }

    private static Archive<?> getResourceB() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, RESOURCE_B);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(RESOURCE_B);
                builder.addImportPackages(Runtime.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    static class TestContentHandler extends AbstractContentHandler {

        @Override
        public Map<String, Object> process(ContentCapability ccap) throws IOException {
            ResourceIdentity resid = ccap.getResource().getIdentity();
            Map<String, Object> atts = new HashMap<>(ccap.getAttributes());
            if (atts.get(ContentNamespace.CAPABILITY_STREAM_ATTRIBUTE) != null) {
                atts.clear();
                atts.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, new URL("file://" + resid.getSymbolicName() + "-" + resid.getVersion()));
            }
            return atts;
        }
    }
}
