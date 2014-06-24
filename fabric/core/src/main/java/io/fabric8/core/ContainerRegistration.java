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

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JOLOKIA_AGENT_URL;
import io.fabric8.api.AttributeKey;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.LockHandle;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.AbstractCreateOptions;
import io.fabric8.spi.AbstractURLServiceEndpoint;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.HttpAttributeProvider;
import io.fabric8.spi.JMXAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.RuntimeType;

/**
 * Registers the current container.
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ContainerRegistration.class)
public class ContainerRegistration extends AbstractComponent {

    @Reference(referenceInterface = BootConfiguration.class)
    private final ValidatingReference<BootConfiguration> bootConfiguration = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerLockManager.class)
    private final ValidatingReference<ContainerLockManager> containerLocks = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = HttpAttributeProvider.class)
    private final ValidatingReference<HttpAttributeProvider> httpProvider = new ValidatingReference<>();
    @Reference(referenceInterface = JMXAttributeProvider.class)
    private final ValidatingReference<JMXAttributeProvider> jmxProvider = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private ContainerIdentity currentContainerId;

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
        currentContainerId = ContainerIdentity.createFrom(runtimeService.get().getRuntimeIdentity());
        LockHandle writeLock = aquireWriteLock(currentContainerId);
        try {
            registerContainer(currentContainerId);
        } finally {
            writeLock.unlock();
        }

    }

    Container getCurrentContainer() {
        ContainerRegistry registry = containerRegistry.get();
        return registry.getContainer(currentContainerId);
    }

    private Container registerContainer(ContainerIdentity identity) {
        ContainerRegistry registry = containerRegistry.get();

        final Map<AttributeKey<?>, Object> httpAttributes = httpProvider.get().getAttributes();
        final Map<AttributeKey<?>, Object> jmxAttributes = jmxProvider.get().getAttributes();
        final String jolokiaEndpointURL;

        // Create Initial Endpoints
        Set<ServiceEndpoint> endpoints = new LinkedHashSet<>();

        // Http endpoint
        String httpEndpointURL = (String) httpAttributes.get(ATTRIBUTE_KEY_HTTP_URL);
        Map<AttributeKey<?>, Object> httpAtts = new HashMap<>(httpAttributes);
        httpAtts.put(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL, httpEndpointURL);
        endpoints.add(new AbstractURLServiceEndpoint(URLServiceEndpoint.HTTP_SERVICE_ENDPOINT_IDENTITY, httpAtts));

        // Jolokia endpoint
        httpAtts.put(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL, jolokiaEndpointURL = httpEndpointURL + "/fabric8/jolokia");
        endpoints.add(new AbstractURLServiceEndpoint(URLServiceEndpoint.JOLOKIA_SERVICE_ENDPOINT_IDENTITY, httpAtts));

        // JMX endpoint
        String jmxEndpointURL = (String) jmxAttributes.get(ATTRIBUTE_KEY_JMX_SERVER_URL);
        Map<AttributeKey<?>, Object> jmxAtts = new HashMap<>(jmxAttributes);
        jmxAtts.put(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL, jmxEndpointURL);
        endpoints.add(new AbstractURLServiceEndpoint(URLServiceEndpoint.JMX_SERVICE_ENDPOINT_IDENTITY, jmxAtts));

        // Get boot profile version
        VersionIdentity bootVersion = bootConfiguration.get().getVersion();

        // Get boot profiles
        List<ProfileIdentity> profiles = new ArrayList<>(bootConfiguration.get().getProfiles());

        CreateOptions options = new AbstractCreateOptions() {
            {
                addAttributes(httpAttributes);
                addAttributes(jmxAttributes);
                addAttribute(ATTRIBUTE_KEY_JOLOKIA_AGENT_URL, jolokiaEndpointURL);
            }

            @Override
            public RuntimeType getRuntimeType() {
                return RuntimeType.getRuntimeType();
            }
        };

        return registry.createContainer(null, identity, options, bootVersion, profiles, endpoints);
    }

    private LockHandle aquireWriteLock(ContainerIdentity identity) {
        return containerLocks.get().aquireWriteLock(identity);
    }

    void bindBootConfiguration(BootConfiguration service) {
        bootConfiguration.bind(service);
    }
    void unbindBootConfiguration(BootConfiguration service) {
        bootConfiguration.unbind(service);
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

    void bindHttpProvider(HttpAttributeProvider service) {
        httpProvider.bind(service);
    }
    void unbindHttpProvider(HttpAttributeProvider service) {
        httpProvider.unbind(service);
    }

    void bindJmxProvider(JMXAttributeProvider service) {
        jmxProvider.bind(service);
    }
    void unbindJmxProvider(JMXAttributeProvider service) {
        jmxProvider.unbind(service);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }
}
