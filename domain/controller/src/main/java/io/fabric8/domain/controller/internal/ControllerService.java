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
package io.fabric8.domain.controller.internal;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.domain.controller.Controller;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.ProvisionException;
import org.jboss.gravia.resource.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central {@link Controller}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(configurationPid = Controller.CONTROLLER_SERVICE_PID, policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(Controller.class)
public final class ControllerService extends AbstractComponent implements Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerService.class);

    @Activate
    void activate(Map<String, ?> config) throws ProvisionException {
        activateComponent();
    }

    // @Modified not implemented - we get a new component with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container createContainer(CreateOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container createContainer(ContainerIdentity parentId, CreateOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ContainerIdentity> getContainerIdentities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getCurrentContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container destroyContainer(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean pingContainer(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container joinFabric(ContainerIdentity identity, JoinOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container leaveFabric(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, Version version, ProvisionEventListener listener) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container addProfiles(ContainerIdentity identity, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Profile getEffectiveProfile(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(ContainerIdentity identity, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity<?> endpointId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> getFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> clearFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }
}
