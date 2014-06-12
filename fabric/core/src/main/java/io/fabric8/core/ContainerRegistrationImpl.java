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
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.LockHandle;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.AbstractCreateOptions;
import io.fabric8.spi.AbstractJMXServiceEndpoint;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.ContainerRegistration;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.RuntimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ContainerRegistration.class)
public class ContainerRegistrationImpl extends AbstractComponent implements ContainerRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRegistrationImpl.class);

    @Reference(referenceInterface = AttributeProvider.class, target = "(&(type="+ContainerAttributes.TYPE+")(classifier=network))",
            bind = "bindAttributeProvider", unbind = "unbindAttributeProvider")
    private AttributeProvider networkAttributeProvider;

    @Reference(referenceInterface = AttributeProvider.class, target = "(&(type="+ContainerAttributes.TYPE+")(classifier=jmx))",
            bind = "bindAttributeProvider", unbind = "unbindAttributeProvider")
    private AttributeProvider jmxAttributeProvider;

    @Reference(referenceInterface = AttributeProvider.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            bind = "bindAttributeProvider", unbind = "unbindAttributeProvider", target = "(type=" + ContainerAttributes.TYPE + ")")
    private final List<AttributeProvider> attributeProviders = new CopyOnWriteArrayList<>();

    @Reference(referenceInterface = BootConfiguration.class)
    private final ValidatingReference<BootConfiguration> bootConfiguration = new ValidatingReference<>();

    @Reference(referenceInterface = ContainerLockManager.class)
    private final ValidatingReference<ContainerLockManager> containerLocks = new ValidatingReference<>();

    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private final Map<AttributeKey<?>, Object> attributes = new HashMap<>();
    private ContainerIdentity currentIdentity;



    @Activate
    void activate() {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void activateInternal() {
        // Create the current container
        currentIdentity = ContainerIdentity.createFrom(runtimeService.get().getRuntimeIdentity());

        LockHandle writeLock = aquireWriteLock(currentIdentity);
        try {
            registerContainer(currentIdentity);
        } finally {
            writeLock.unlock();
        }

    }

    private void registerContainer(ContainerIdentity identity) {
        ContainerRegistry registry = containerRegistry.get();
        //Create Initial Endpoints
        Set<ServiceEndpoint> endpoints = new LinkedHashSet<>();
        if (attributes.containsKey(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL)) {
            endpoints.add(new AbstractJMXServiceEndpoint(ServiceEndpointIdentity.create("jmx", JMXServiceEndpoint.class), (String) attributes.get(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL)));
        }

        Container container = registry.getContainer(currentIdentity);
        if (container == null) {
            // Get boot profile version
            Version bootVersion = bootConfiguration.get().getVersion();

            // Get boot profiles
            List<String> profiles = new ArrayList<>(bootConfiguration.get().getProfiles());

            CreateOptions options = new AbstractCreateOptions() {
                {
                    addAttributes(attributes);
                }

                @Override
                public RuntimeType getRuntimeType() {
                    return RuntimeType.getRuntimeType();
                }
            };

            container = registry.createContainer(null, currentIdentity, options, bootVersion, profiles, endpoints);
        }

        registry.setServiceEndpoints(currentIdentity, endpoints);
    }

    private void addAttributes(AttributeProvider provider) {
        for (Map.Entry<AttributeKey<?>, Object> entry : provider.getAttributes().entrySet()) {
            attributes.put(entry.getKey(), entry.getValue());
        }
    }

    private void removeAttributes(AttributeProvider provider) {
        for (Map.Entry<AttributeKey<?>, Object> entry : provider.getAttributes().entrySet()) {
            attributes.remove(entry.getKey());
        }
    }

    private LockHandle aquireWriteLock(ContainerIdentity identity) {
        return containerLocks.get().aquireWriteLock(identity);
    }

    private LockHandle aquireReadLock(ContainerIdentity identity) {
        return containerLocks.get().aquireReadLock(identity);
    }

    void bindAttributeProvider(AttributeProvider provider) {
        addAttributes(provider);
        attributeProviders.add(provider);
    }

    void unbindAttributeProvider(AttributeProvider provider) {
        attributeProviders.remove(provider);
        removeAttributes(provider);
    }


    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }

    void bindContainerLocks(ContainerLockManager service) {
        containerLocks.bind(service);
    }
    void unbindContainerLocks(ContainerLockManager service) {
        containerLocks.unbind(service);
    }

    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }
    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
    }


    void bindBootConfiguration(BootConfiguration service) {
        bootConfiguration.bind(service);
    }
    void unbindBootConfiguration(BootConfiguration service) {
        bootConfiguration.unbind(service);
    }
}
