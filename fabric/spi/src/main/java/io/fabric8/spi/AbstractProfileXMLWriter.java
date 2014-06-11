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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Configuration;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.RequirementItem;
import io.fabric8.api.ResourceItem;
import io.fabric8.spi.ProfilesNamespace100.Attribute;
import io.fabric8.spi.ProfilesNamespace100.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.gravia.repository.RepositoryWriter.ContentHandler;
import org.jboss.gravia.repository.spi.AbstractRepositoryXMLWriter;
import org.jboss.gravia.resource.spi.AttributeValueHandler;
import org.jboss.gravia.resource.spi.AttributeValueHandler.AttributeValue;
import org.jboss.gravia.utils.IllegalArgumentAssertion;


/**
 * Write profile content to XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2014
 */
public abstract class AbstractProfileXMLWriter implements ProfileWriter {

    private final XMLStreamWriter writer;
    private final ContentHandler contentHandler;

    protected AbstractProfileXMLWriter(OutputStream outputStream) {
        this(outputStream, null);
    }

    protected AbstractProfileXMLWriter(OutputStream outputStream, ContentHandler contentHandler) {
        IllegalArgumentAssertion.assertNotNull(outputStream, "outputStream");
        this.writer = createXMLStreamWriter(outputStream);
        this.contentHandler = contentHandler;
    }

    protected abstract XMLStreamWriter createXMLStreamWriter(OutputStream outputStream);

    @Override
    public void writeProfileVersion(LinkedProfileVersion linkedVersion) throws IOException {
        try {
            writer.writeStartDocument();
            writer.setDefaultNamespace(ProfilesNamespace100.PROFILES_NAMESPACE);
            writer.writeStartElement(Element.PROFILES.getLocalName());
            writer.writeDefaultNamespace(ProfilesNamespace100.PROFILES_NAMESPACE);
            writer.writeAttribute(Attribute.VERSION.getLocalName(), linkedVersion.getIdentity().toString());
            Set<String> processed = new HashSet<>();
            for (String profileId : linkedVersion.getProfileIdentities()) {
                Profile profile = linkedVersion.getLinkedProfile(profileId);
                writeProfile(linkedVersion, profile, processed);

            }
        } catch (XMLStreamException ex) {
            throw new IOException("Cannot write repository element", ex);
        }
    }

    private void writeProfile(LinkedProfileVersion linkedVersion, Profile profile, Set<String> processed) throws IOException {
        IllegalArgumentAssertion.assertNotNull(linkedVersion, "linkedVersion");
        IllegalArgumentAssertion.assertNotNull(profile, "profile");
        IllegalArgumentAssertion.assertNotNull(processed, "processed");
        String identity = profile.getIdentity();
        if (!processed.contains(identity)) {
            for (String parentId : profile.getParents()) {
                Profile parentProfile = linkedVersion.getLinkedProfile(parentId);
                writeProfile(linkedVersion, parentProfile, processed);
            }
            try {
                writer.writeStartElement(Element.PROFILE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), profile.getIdentity());
                writeProfileAttributes(profile.getAttributes());
                writeProfileParents(profile.getParents());
                writeProfileItems(profile, profile.getProfileItems(null));
                writer.writeEndElement();
            } catch (XMLStreamException ex) {
                throw new IllegalStateException("Cannot initialize repository writer", ex);
            }
            processed.add(identity);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException ex) {
            throw new IOException("Cannot write repository element", ex);
        }
    }

    private void writeProfileAttributes(Map<AttributeKey<?>, Object> attributes) throws XMLStreamException {
        for (Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            writer.writeStartElement(Element.ATTRIBUTE.getLocalName());
            writer.writeAttribute(Attribute.KEY.getLocalName(), entry.getKey().getCanonicalForm());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().toString());
            writer.writeEndElement();
        }
    }

    private void writeProfileParents(List<String> parents) throws XMLStreamException {
        for (String parentId : parents) {
            writer.writeStartElement(Element.PARENT.getLocalName());
            writer.writeAttribute(Attribute.ID.getLocalName(), parentId);
            writer.writeEndElement();
        }
    }

    private void writeProfileItems(Profile profile, List<ProfileItem> items) throws XMLStreamException, IOException {
        for (ProfileItem item : items) {
            if (item instanceof ConfigurationItem) {
                ConfigurationItem confItem = (ConfigurationItem) item;
                writer.writeStartElement(Element.CONFIGURATIONITEM.getLocalName());
                writer.writeAttribute(Attribute.ID.getLocalName(), item.getIdentity());
                for (Configuration config : confItem.getConfigurations(null)) {
                    writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                    writer.writeAttribute(Attribute.ID.getLocalName(), config.getMergeId());
                    writeAttributes(config.getAttributes());
                    writeDirectives(config.getDirectives());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            } else if (item instanceof ResourceItem) {
                ResourceItem resItem = (ResourceItem) item;
                writer.writeStartElement(Element.RESOURCEITEM.getLocalName());
                writer.writeAttribute(Attribute.ID.getLocalName(), item.getIdentity());
                contentHandler.addContextItem(Profile.class, profile);
                contentHandler.addContextItem(ResourceItem.class, resItem);
                AbstractRepositoryXMLWriter.writeResource(writer, resItem.getResource(), contentHandler);
                writer.writeEndElement();
            } else if (item instanceof RequirementItem) {
                RequirementItem reqItem = (RequirementItem) item;
                writer.writeStartElement(Element.REQUIREMENTITEM.getLocalName());
                writer.writeAttribute(Attribute.ID.getLocalName(), item.getIdentity());
                AbstractRepositoryXMLWriter.writeRequirement(writer, reqItem.getRequirement());
                writer.writeEndElement();
            } else {
                throw new IllegalArgumentException("Unsupported profile item: " + item);
            }
        }
    }

    private void writeAttributes(Map<String, Object> attributes) throws XMLStreamException {
        for (Entry<String, Object> entry : attributes.entrySet()) {
            AttributeValue attval = AttributeValue.create(entry.getValue());
            writer.writeStartElement(Element.ATTRIBUTE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getKey());
            if (attval.isListType()) {
                writer.writeAttribute(Attribute.VALUE.getLocalName(), attval.getValueString());
                writer.writeAttribute(Attribute.TYPE.getLocalName(), "List<" + attval.getType() + ">");
            } else {
                writer.writeAttribute(Attribute.VALUE.getLocalName(), attval.getValueString());
                if (attval.getType() != AttributeValueHandler.Type.String) {
                    writer.writeAttribute(Attribute.TYPE.getLocalName(), attval.getType().toString());
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeDirectives(Map<String, String> directives) throws XMLStreamException {
        for (Entry<String, String> entry : directives.entrySet()) {
            writer.writeStartElement(Element.DIRECTIVE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getKey());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue());
            writer.writeEndElement();
        }
    }
}
