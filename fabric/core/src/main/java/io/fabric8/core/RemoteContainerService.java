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
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.process.ManagedProcess;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.Set;
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

/**
 * The {@link RemoteContainerService}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(RemoteContainerService.class)
public final class RemoteContainerService extends AbstractComponent {

    @Reference(referenceInterface = Agent.class)
    private final ValidatingReference<Agent> agent = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

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
        return new ManagedContainerState(process).immutableContainer();
    }

    // [TODO] provision event for remote containers
    Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        Future<ManagedProcess> future = agent.get().startProcess(getProcessIdentity(identity));
        ManagedProcess process;
        try {
            process = future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot get future process value after start for: " + identity, ex);
        }
        return new ManagedContainerState(process).immutableContainer();
    }

    Container stopContainer(ContainerIdentity identity) {
        Future<ManagedProcess> future = agent.get().stopProcess(getProcessIdentity(identity));
        ManagedProcess process;
        try {
            process = future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot get future process value after stop for: " + identity, ex);
        }
        return new ManagedContainerState(process).immutableContainer();
    }

    Container destroyContainer(ContainerIdentity identity) {

        ManagedContainerState cntState = getRequiredContainerState(identity);
        Set<ContainerIdentity> childIdentities = cntState.getChildIdentities();
        IllegalStateAssertion.assertTrue(childIdentities.isEmpty(), "Cannot destroy a container that has active child containers: " + cntState);

        // Stop the container
        if (cntState.getState() == State.STARTED) {
            stopContainer(identity);
        }

        ManagedProcess process = cntState.getManagedProcess();
        process = agent.get().destroyProcess(process.getIdentity());
        return cntState.immutableContainer();
    }


    void bindAgent(Agent service) {
        agent.bind(service);
    }
    void unbindAgent(Agent service) {
        agent.unbind(service);
    }

    private ManagedContainerState getRequiredContainerState(ContainerIdentity identity) {
        ManagedProcess process = agent.get().getManagedProcess(getProcessIdentity(identity));
        IllegalStateAssertion.assertNotNull(process, "Cannot obtain managed process: " + identity);
        return new ManagedContainerState(process);
    }

    private ProcessIdentity getProcessIdentity(ContainerIdentity identity) {
        return ProcessIdentity.create(identity.getSymbolicName());
    }

    static class ManagedContainerState extends AttributeSupport {

        private final ContainerIdentity identity;
        private final ManagedProcess process;
        private final CreateOptions createOptions;


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
            this.createOptions = (CreateOptions) process.getCreateOptions();
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
            ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, createOptions.getRuntimeType(), getAttributes(), getState());
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
