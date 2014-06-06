/*
 * #%L
 * Fabric8 :: Core
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
package io.fabric8.core;

import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.process.ManagedProcess;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.ProvisionException;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.LifecycleException;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RemoteContainerService}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(RemoteContainerService.class)
public final class RemoteContainerService extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteContainerService.class);

    @Reference(referenceInterface = Agent.class)
    private final ValidatingReference<Agent> agent = new ValidatingReference<>();

    private final Map<ContainerIdentity, ManagedContainerState> containers = new ConcurrentHashMap<ContainerIdentity, ManagedContainerState>();

    @Activate
    void activate() {
        activateComponent();
    }

    // @Modified not implemented - we get a new component with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    Container createContainer(CreateOptions options) {
        assertValid();
        return createContainerInternal(null, options);
    }

    Container createContainer(ContainerIdentity parentId, CreateOptions options) {
        assertValid();
        return createContainerInternal(parentId, options);
    }

    private Container createContainerInternal(ContainerIdentity parentId, CreateOptions options) {
        IllegalArgumentAssertion.assertTrue(options instanceof ProcessOptions, "Invalid process options: " + options);
        ProcessOptions processOptions = (ProcessOptions) options;
        ManagedProcess process = agent.get().createProcess(processOptions);
        ManagedContainerState cntState = new ManagedContainerState(process);
        containers.put(cntState.getIdentity(), cntState);
        return cntState.immutableContainer();
    }

    Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        ManagedContainerState cntState = getRequiredContainerState(identity);
        LOGGER.info("Start container: {}", cntState);
        ManagedProcess process = cntState.getManagedProcess();
        Future<ManagedProcess> future = agent.get().startProcess(process.getIdentity());
        ManagedProcess result;
        try {
            result = future.get(10, TimeUnit.SECONDS);
            containers.put(cntState.getIdentity(), cntState = new ManagedContainerState(result));
        } catch (Exception ex) {
            throw new LifecycleException("Cannot get future process value after start for: " + process, ex);
        }
        return cntState.immutableContainer();
    }

    Container stopContainer(ContainerIdentity identity) {
        ManagedContainerState cntState = getRequiredContainerState(identity);
        LOGGER.info("Stop container: {}", cntState);
        ManagedProcess process = cntState.getManagedProcess();
        Future<ManagedProcess> future = agent.get().stopProcess(process.getIdentity());
        ManagedProcess result;
        try {
            result = future.get(10, TimeUnit.SECONDS);
            containers.put(cntState.getIdentity(), cntState = new ManagedContainerState(result));
        } catch (Exception ex) {
            throw new LifecycleException("Cannot get future process value after stop for: " + process, ex);
        }
        return cntState.immutableContainer();
    }

    Container destroyContainer(ContainerIdentity identity) {
        ManagedContainerState cntState = getRequiredContainerState(identity);
        Set<ContainerIdentity> childIdentities = cntState.getChildIdentities();
        IllegalStateAssertion.assertTrue(childIdentities.isEmpty(), "Cannot destroy a container that has active child containers: " + cntState);

        // Stop the container
        if (cntState.getState() == State.STARTED) {
            stopContainer(identity);
        }
        LOGGER.info("Destroy container: {}", cntState);
        containers.remove(identity);
        ManagedProcess process = cntState.getManagedProcess();
        process = agent.get().destroyProcess(process.getIdentity());
        containers.put(cntState.getIdentity(), cntState = new ManagedContainerState(process));
        return cntState.immutableContainer();
    }


    void bindAgent(Agent service) {
        agent.bind(service);
    }
    void unbindAgent(Agent service) {
        agent.unbind(service);
    }

    private ManagedContainerState getRequiredContainerState(ContainerIdentity identity) {
        ManagedContainerState cntState = containers.get(identity);
        IllegalStateAssertion.assertNotNull(cntState, "Container not registered: " + identity);
        return cntState;
    }

    static class ManagedContainerState extends AttributeSupport {

        private final ContainerIdentity identity;
        private final ManagedProcess process;

        private Version profileVersion;
        /*
        private Set<ServiceEndpointIdentity<?>> endpoints = new HashSet<>();
        private Set<ContainerIdentity> children = new HashSet<>();
        private List<String> profiles = new ArrayList<>();
        private ManagedContainerState parentState;
        */

        ManagedContainerState(ManagedProcess process) {
            super(process.getAttributes(), true);
            this.identity = ContainerIdentity.create(process.getIdentity().getName());
            this.process = process;
        }

        Set<ContainerIdentity> getChildIdentities() {
            return Collections.emptySet();
        }

        ManagedProcess getManagedProcess() {
            return process;
        }

        ContainerIdentity getIdentity() {
            return identity;
        }

        State getState() {
            return State.valueOf(process.getState().name());
        }

        ImmutableContainer immutableContainer() {
            ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, getAttributes(), getState());
            // [TODO] child, profiles, endpoints on managed container
            //builder.addParent(parentState != null ? parentState.getIdentity() : null);
            //builder.addProfileVersion(profileVersion);
            //builder.addChildren(getChildIdentities());
            //builder.addProfiles(getProfileIdentities());
            //builder.addServiceEndpoints(getServiceEndpointIdentities());
            return builder.build();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ManagedContainerState)) return false;
            ManagedContainerState other = (ManagedContainerState) obj;
            return other.identity.equals(identity);
        }

        @Override
        public int hashCode() {
            return identity.hashCode();
        }

        @Override
        public String toString() {
            return "ContainerState[id=" + identity + ",state=" + process.getState() + ",version=" + profileVersion + "]";
        }
    }
}
