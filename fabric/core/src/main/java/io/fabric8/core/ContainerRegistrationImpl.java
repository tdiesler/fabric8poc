/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.core;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.ContainerRegistration;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ContainerRegistration.class)
public class ContainerRegistrationImpl extends AbstractComponent implements ContainerRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRegistrationImpl.class);

    @Reference(referenceInterface = AttributeProvider.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            bind = "bindAttributeProvider", unbind = "unbindAttributeProvider", target = "(type=" + ContainerAttributes.TYPE + ")")

    private final List<AttributeProvider> attributeProviders = new CopyOnWriteArrayList<>();

    @Activate
    void activate() {
        for (AttributeProvider provider : attributeProviders) {
            addAttributes(provider);
        }
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public <T> void attributeAdded(AttributeKey<T> key, T value) {
        assertValid();
        LOGGER.info("Attribute Added:{} - {}.", key, value);
    }

    @Override
    public <T> void attributeChanged(AttributeKey<T> key, T value) {
        assertValid();
        LOGGER.info("Attribute Changed:{} - {}.", key, value);
    }

    @Override
    public <T> void attributeRemoved(AttributeKey<T> key, T value) {
        assertValid();
        LOGGER.info("Attribute Removed:{} - {}.", key, value);
    }

    private void addAttributes(AttributeProvider provider) {
        for (Map.Entry<AttributeKey<?>, Object> entry : provider.getAttributes().entrySet()) {
            LOGGER.info("Attribute Added:{} - {}.", entry.getKey(), entry.getValue());
        }
    }

    private void removeAttributes(AttributeProvider provider) {
        for (Map.Entry<AttributeKey<?>, Object> entry : provider.getAttributes().entrySet()) {
            LOGGER.info("Removed Added:{} - {}.", entry.getKey(), entry.getValue());
        }
    }

    void bindAttributeProvider(AttributeProvider provider) {
        addAttributes(provider);
        provider.addListener(this);
        attributeProviders.add(provider);
    }

    void unbindAttributeProvider(AttributeProvider provider) {
        provider.removeListener(this);
        attributeProviders.remove(provider);
        removeAttributes(provider);
    }
}
