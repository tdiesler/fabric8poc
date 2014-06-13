/*
 * #%L
 * Gravia :: Profile
 * %%
 * Copyright (C) 2012 - 2014 JBoss by Red Hat
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
package io.fabric8.spi;

import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.gravia.repository.spi.AbstractRepositoryXMLReader.assertEndElement;
import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.RequirementItem;
import io.fabric8.api.ResourceItem;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.ProfilesNamespace100.Attribute;
import io.fabric8.spi.ProfilesNamespace100.Element;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.gravia.repository.spi.AbstractRepositoryXMLReader;
import org.jboss.gravia.resource.DefaultRequirementBuilder;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.RequirementBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * Read profile content from XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2014
 */
public abstract class AbstractProfileXMLReader implements ProfileReader {

    private final Map<String, String> attributes = new HashMap<String, String>();
    private final VersionIdentity profileVersion;
    private final XMLStreamReader reader;

    public AbstractProfileXMLReader(InputStream inputStream) {
        IllegalArgumentAssertion.assertNotNull(inputStream, "inputStream");
        reader = createXMLStreamReader(inputStream);
        try {
            reader.require(START_DOCUMENT, null, null);
            reader.nextTag();
            reader.require(START_ELEMENT, ProfilesNamespace100.PROFILES_NAMESPACE, Element.PROFILES.getLocalName());
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read resource element: " + reader.getLocation(), ex);
        }
        profileVersion = VersionIdentity.createFrom(attributes.get(Attribute.VERSION.getLocalName()));
    }

    protected abstract XMLStreamReader createXMLStreamReader(InputStream inputSteam);

    protected abstract ResourceBuilder createResourceBuilder();

    @Override
    public VersionIdentity getProfileVersion() {
        return profileVersion;
    }

    @Override
    public Profile nextProfile() {
        try {
            while (reader.hasNext() && reader.nextTag() == START_ELEMENT) {
                Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case PROFILE:
                        return readProfileElement(reader);
                    default:
                        throw new IllegalArgumentException("Unsupported element: " + reader.getLocalName());
                }
            }
        } catch (XMLStreamException ex) {
            throw new IllegalStateException("Cannot read profile element: " + reader.getLocation(), ex);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (XMLStreamException ex) {
            // ignore
        }
    }

    private Profile readProfileElement(XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getAttributeValue(null, Attribute.NAME.toString());
        ProfileBuilder builder = new DefaultProfileBuilder(name).profileVersion(getProfileVersion());
        while (reader.hasNext() && reader.nextTag() == START_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case ATTRIBUTE:
                readProfileAttributeElement(reader, builder);
                break;
            case CONFIGURATIONITEM:
                ConfigurationItem confItem = readConfigurationItem(reader);
                builder.addProfileItem(confItem);
                break;
            case PARENT:
                String parentId = reader.getAttributeValue(null, Attribute.ID.toString());
                builder.addParentProfile(parentId);
                assertEndElement(reader, Element.PARENT.getLocalName());
                break;
            case RESOURCEITEM:
                ResourceItem resItem = readResourceItem(reader);
                builder.addProfileItem(resItem);
                break;
            case REQUIREMENTITEM:
                RequirementItem reqItem = readRequirementItem(reader);
                builder.addProfileItem(reqItem);
                break;
            default:
                throw new IllegalArgumentException("Unsupported element: " + reader.getLocalName());
            }
        }
        assertEndElement(reader, Element.PROFILE.getLocalName());
        return builder.getProfile();
    }

    private void readProfileAttributeElement(XMLStreamReader reader, ProfileBuilder builder) throws XMLStreamException {
        String keystr = reader.getAttributeValue(null, Attribute.KEY.toString());
        Object valstr = reader.getAttributeValue(null, Attribute.VALUE.toString());
        AttributeKey<Object> key = AttributeKey.createFrom(keystr);
        Object value = key.getFactory().createFrom(valstr);
        builder.addAttribute(key, value);
        assertEndElement(reader, Element.ATTRIBUTE.getLocalName());
    }

    private ConfigurationItem readConfigurationItem(XMLStreamReader reader) throws XMLStreamException {
        String itemId = reader.getAttributeValue(null, Attribute.ID.toString());
        DefaultConfigurationItemBuilder builder = new DefaultConfigurationItemBuilder(itemId);
        while (reader.hasNext() && reader.nextTag() == START_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case CONFIGURATION:
                readConfiguration(reader, builder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported element: " + reader.getLocalName());
            }
        }
        assertEndElement(reader, Element.CONFIGURATIONITEM.getLocalName());
        return builder.getConfigurationItem();
    }

    private ResourceItem readResourceItem(XMLStreamReader reader) throws XMLStreamException {
        String itemId = reader.getAttributeValue(null, Attribute.ID.toString());
        DefaultResourceBuilder builder = new DefaultResourceBuilder();
        Resource resource = AbstractRepositoryXMLReader.nextResource(reader, builder);
        assertEndElement(reader, Element.RESOURCEITEM.getLocalName());
        return new DefaultResourceItem(itemId, resource);
    }

    private RequirementItem readRequirementItem(XMLStreamReader reader) throws XMLStreamException {
        String itemId = reader.getAttributeValue(null, Attribute.ID.toString());
        IllegalStateAssertion.assertTrue(reader.hasNext() && reader.nextTag() == START_ELEMENT, "Expected requirement start element, but got: " + reader.getLocalName());
        Element element = Element.forName(reader.getLocalName());
        IllegalStateAssertion.assertEquals(Element.REQUIREMENT, element, "Expected requirement element, but got: " + element);
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        AbstractRepositoryXMLReader.readAttributesAndDirectives(reader, atts, dirs);
        String nsvalue = (String) atts.get(IdentityNamespace.IDENTITY_NAMESPACE);
        RequirementBuilder builder = new DefaultRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, nsvalue);
        builder.getAttributes().putAll(atts);
        builder.getDirectives().putAll(dirs);
        Requirement requirement = builder.getRequirement();
        assertEndElement(reader, Element.REQUIREMENTITEM.getLocalName());
        return new DefaultRequirementItem(itemId, requirement);
    }

    private void readConfiguration(XMLStreamReader reader, ConfigurationItemBuilder builder) throws XMLStreamException {
        String mergeId = reader.getAttributeValue(null, Attribute.ID.toString());
        Map<String, Object> atts = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        AbstractRepositoryXMLReader.readAttributesAndDirectives(reader, atts, dirs);
        builder.addConfiguration(mergeId, atts, dirs);
        assertEndElement(reader, Element.CONFIGURATION.getLocalName());
    }
}
