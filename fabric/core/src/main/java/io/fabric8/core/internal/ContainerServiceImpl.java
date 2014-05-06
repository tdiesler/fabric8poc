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
package io.fabric8.core.internal;

import static io.fabric8.api.Constants.CURRENT_CONTAINER_IDENTITY;
import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.Host;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.ServiceLocator;
import io.fabric8.core.internal.ProfileServiceImpl.ProfileState;
import io.fabric8.core.internal.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ClusterDataStore;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.ContainerHandle;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.DefaultContainerCreateHandler;
import io.fabric8.spi.DefaultCreateOptions;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.ManagedCreateOptions;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.IllegalStateAssertion;
import io.fabric8.spi.utils.ProfileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.utils.NotNullException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The internal {@link ContainerService}
 *
 * It is the owner of all {@link Container} instances.
 * Internally it maintains mutable versions these instances. All mutating operations must go through this service.
 * The public API returns shallow immutable {@link Container} instances, mutable {@link ContainerState} instances must not leak outside this service.
 *
 * This service is protected by a permit.
 * Access without the calling client holding a permit is considered a programming error.
 *
 * Concurrency & Locking Strategy
 * ------------------------------
 *
 * Read access to {@link Container} can happen concurrently.
 * Each {@link Container} instance is associated with a {@link ReentrantReadWriteLock}
 * The mutable {@link ContainerState} must synchronize concurrent read operations, write operations are synchronized by the lock on {@link Container}
 *
 * A client may explicitly acquire a write lock for a {@link Container}.
 * Obtaining a write lock for a {@link Container} also obtains a write lock on the associated {@link ProfileVersion}.
 * This is necessary when exclusive access to {@link Container} content is required. For example when making multiple calls
 * as part of one operation - while doing so the {@link Container} and its associated {@link ProfileVersion} must be locked and cannot change.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(service = { ContainerService.class }, configurationPid = Container.CONTAINER_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerServiceImpl.class);

    private final ValidatingReference<ClusterDataStore> clusterData = new ValidatingReference<ClusterDataStore>();
    private final ValidatingReference<ConfigurationManager> configManager = new ValidatingReference<ConfigurationManager>();
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();
    private final Set<ContainerCreateHandler> createHandlers = new HashSet<ContainerCreateHandler>();
    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();

    private String configToken;

    @Activate
    void activate(Map<String, ?> config) {
        configToken = (String) config.get(Container.CNFKEY_CONFIG_TOKEN);
        activateInternal();
        activateComponent(PERMIT, this);
    }

    // @Modified not implemented - we get a new component with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
        for (ServiceRegistration<?> sreg : registrations) {
            sreg.unregister();
        }
    }

    private void activateInternal() {

        // Create the current container
        ContainerState currentCnt = containerRegistry.get().getContainer(CURRENT_CONTAINER_IDENTITY);
        if (currentCnt == null) {
            DefaultCreateOptions options = new DefaultCreateOptions();
            List<ContainerHandle> handles = Collections.emptyList();
            currentCnt = new ContainerState(null, CURRENT_CONTAINER_IDENTITY, options, handles, configToken);
            LOGGER.info("Create current container: {}", currentCnt);
            containerRegistry.get().addContainer(currentCnt);
        }

        // Register a listener for profile update events
        ProfileEventListener listener = new ProfileEventListener() {

            @Override
            public void processEvent(ProfileEvent event) {
                Profile profile = event.getSource();
                if (event.getType() != ProfileEvent.EventType.UPDATED)
                    return;

                LOGGER.info("Profile updated: {}", profile);

                PermitManager permitManager = ServiceLocator.getRequiredService(PermitManager.class);
                Permit<ContainerService> permit = permitManager.aquirePermit(ContainerService.PERMIT, false);
                try {
                    ContainerServiceImpl service = (ContainerServiceImpl) permit.getInstance();
                    String profileId = profile.getIdentity();
                    for (ContainerState cntState : service.getContainerStates(null)) {
                        if (cntState.getProfileIdentities().contains(profileId)) {
                            LockHandle writeLock = cntState.aquireWriteLock();
                            try {
                                service.updateProfileInternal(cntState, profileId, null);
                            } finally {
                                writeLock.unlock();
                            }
                        }
                    }
                } finally {
                    permit.release();
                }
            }
        };

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        registrations.add(syscontext.registerService(ProfileEventListener.class, listener, null));
    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        return cntState.aquireWriteLock();
    }

    @Override
    public Container createContainer(CreateOptions options) {
        assertValid();
        return createContainerInternal(null, options);
    }

    @Override
    public Container createContainer(ContainerIdentity parentId, CreateOptions options) {
        assertValid();
        return createContainerInternal(getRequiredContainer(parentId), options);
    }

    private Container createContainerInternal(ContainerState parentState, CreateOptions options) {

        // Every type of {@link CreateOptions}
        List<ContainerHandle> handles = new ArrayList<ContainerHandle>();
        Set<ContainerCreateHandler> handlers = getContainerCreateHandlers();
        if (options instanceof ManagedCreateOptions) {
            ManagedCreateOptions managedOptions = (ManagedCreateOptions) options;
            Class<? extends ContainerCreateHandler> primaryType = managedOptions.getPrimaryHandler();
            ContainerCreateHandler primary = ServiceLocator.awaitService(primaryType, 10, TimeUnit.SECONDS);
            handles.add(primary.create(options));
            handlers.remove(primary);
        }
        for (ContainerCreateHandler handler : handlers) {
            if (handler.accept(options)) {
                handles.add(handler.create(options));
            }
        }
        IllegalStateAssertion.assertFalse(handles.isEmpty(), "Cannot find ContainerCreateHandler that accepts: " + options);
        ContainerIdentity parentId = parentState != null ? parentState.getIdentity() : null;
        ContainerIdentity identity = clusterData.get().createContainerIdentity(parentId, options.getIdentityPrefix());
        ContainerState cntState = new ContainerState(parentState, identity, options, handles, configToken);
        LOGGER.info("Create container: {}", cntState);
        containerRegistry.get().addContainer(cntState);
        return cntState.immutableContainer();
    }

    private Set<ContainerCreateHandler> getContainerCreateHandlers() {
        Set<ContainerCreateHandler> handlers;
        synchronized (createHandlers) {
            handlers = new HashSet<ContainerCreateHandler>(createHandlers);
        }
        return handlers;
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getContainerState(identity);
        return cntState != null ? cntState.immutableContainer() : null;
    }

    @Override
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            LOGGER.info("Start container: {}", cntState);
            for (ContainerHandle handle : cntState.getContainerHandles()) {
                handle.start();
            }
            if (cntState.getProfileVersion() == null) {
                Profile defaultProfile = profileService.get().getDefaultProfile();
                setVersionInternal(cntState, defaultProfile.getVersion(), listener);
                addProfilesInternal(cntState, Collections.singleton(defaultProfile.getIdentity()), listener);
            }

            // Start & provision the container profiles
            cntState.start();
            Set<String> profiles = cntState.getProfileIdentities();
            provisionProfiles(cntState, profiles, listener);

            return cntState.immutableContainer();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            LOGGER.info("Stop container: {}", cntState);
            for (ContainerHandle handle : cntState.getContainerHandles()) {
                handle.stop();
            }
            return cntState.stop().immutableContainer();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Container destroyContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            IllegalStateAssertion.assertTrue(cntState.getChildIdentities().isEmpty(), "Cannot destroy a container that has active child containers: " + identity);

            // Unprovision the associated profiles
            ProfileVersionState versionState = cntState.getProfileVersion();
            if (versionState != null) {
                Set<String> profiles = cntState.getProfileIdentities();
                unprovisionProfiles(cntState, profiles, null);
            }

            LOGGER.info("Destroy container: {}", cntState);
            for (ContainerHandle handle : cntState.getContainerHandles()) {
                handle.destroy();
            }
            containerRegistry.get().removeContainer(identity);
            cntState.destroy();
            return cntState.immutableContainer();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        return containerRegistry.get().getContainerIdentities();
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<Container> result = new HashSet<>();
        for (ContainerState cntState : getContainerStates(identities)) {
            result.add(cntState.immutableContainer());
        }
        return Collections.unmodifiableSet(result);
    }

    private Set<ContainerState> getContainerStates(Set<ContainerIdentity> identities) {
        Set<ContainerState> result = new HashSet<>(containerRegistry.get().getContainers(null));
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Container getCurrentContainer() {
        assertValid();
        ContainerState currentCnt = containerRegistry.get().getContainer(CURRENT_CONTAINER_IDENTITY);
        return currentCnt.immutableContainer();
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, Version version, ProvisionEventListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return setVersionInternal(cntState, version, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container setVersionInternal(ContainerState cntState, Version nextVersion, ProvisionEventListener listener) {

        ProfileServiceImpl profileServiceImpl = (ProfileServiceImpl) profileService.get();
        ProfileVersionState nextVersionState = profileServiceImpl.getRequiredProfileVersion(nextVersion);
        ProfileVersionState prevVersionState = cntState.getProfileVersion();

        LOGGER.info("Set container version: {} <= {}", cntState, nextVersion);

        // Unprovision the previous profiles
        if (prevVersionState != null) {
            unprovisionProfiles(cntState, cntState.getProfileIdentities(), listener);
        }

        // Provision the next profiles
        cntState.setProfileVersion(nextVersionState);
        provisionProfiles(cntState, cntState.getProfileIdentities(), listener);

        return cntState.immutableContainer();
    }

    @Override
    public Container addProfiles(ContainerIdentity identity, Set<String> identities, ProvisionEventListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return addProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container addProfilesInternal(ContainerState cntState, Set<String> identities, ProvisionEventListener listener) {
        LOGGER.info("Add container profiles: {} <= {}", cntState, identities);

        // Provision the profiles
        provisionProfiles(cntState, identities, listener);

        // Update the references
        cntState.addProfiles(identities);
        return cntState.immutableContainer();
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, Set<String> identities, ProvisionEventListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return removeProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container removeProfilesInternal(ContainerState cntState, Set<String> profiles, ProvisionEventListener listener) {
        LOGGER.info("Remove container profiles: {} => {}", cntState, profiles);

        // Unprovision the profiles
        unprovisionProfiles(cntState, profiles, listener);

        // Update the references
        cntState.removeProfiles(profiles);
        return cntState.immutableContainer();
    }

    private void updateProfileInternal(ContainerState cntState, String identity, ProvisionEventListener listener) {

        LOGGER.info("Update container profile: {} <= {}", cntState, identity);

        // Unprovision the profile
        unprovisionProfile(cntState, identity, listener);

        // Provision the profile
        provisionProfile(cntState, identity, listener);
    }

    private void provisionProfiles(ContainerState cntState, Set<String> profiles, ProvisionEventListener listener) {
        if (cntState.getState() == State.STARTED) {
            for (String profile : profiles) {
                provisionProfile(cntState, profile, listener);
            }
        }
    }

    private void provisionProfile(ContainerState cntState, String identity, ProvisionEventListener listener) {

        ProfileState profileState = cntState.getProfileVersion().getRequiredProfile(identity);
        LOGGER.info("Provision profile: {} <= {}", cntState, identity);

        Profile profile = profileState.immutableProfile();
        Container container = cntState.immutableContainer();
        ProvisionEvent event = new ProvisionEvent(container, EventType.PROVISIONING, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);

        // Do the provisioning
        Profile effectiveProfile = ProfileUtils.getEffectiveProfile(profileState.immutableLinkedProfile());
        Set<ConfigurationProfileItem> configItems = effectiveProfile.getProfileItems(ConfigurationProfileItem.class);
        configManager.get().applyConfigurationItems(configItems);

        event = new ProvisionEvent(container, EventType.PROVISIONED, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);
    }

    private void unprovisionProfiles(ContainerState cntState, Set<String> profiles, ProvisionEventListener listener) {
        if (cntState.getState() == State.STARTED) {
            for (String profileId : profiles) {
                unprovisionProfile(cntState, profileId, listener);
            }
        }
    }

    private void unprovisionProfile(ContainerState cntState, String profileId, ProvisionEventListener listener) {
        LOGGER.info("Unprovision profile: {} => {}", cntState, profileId);

        ProfileState profileState = cntState.getProfileVersion().getRequiredProfile(profileId);
        Profile profile = profileState.immutableProfile();
        Container container = cntState.immutableContainer();
        ProvisionEvent event = new ProvisionEvent(container, EventType.REMOVING, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);

        // do the removing

        event = new ProvisionEvent(container, EventType.REMOVED, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);
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
    public <T extends ServiceEndpoint> T getServiceEndpoint(ContainerIdentity identity, Class<T> type) {
        ContainerState containerState = getRequiredContainer(identity);
        return containerState.getServiceEndpoint(type);
    }

    @Override
    public ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity<?> endpointId) {
        ContainerState containerState = getRequiredContainer(identity);
        return containerState.getServiceEndpoint(endpointId);
    }

    @Override
    public List<Failure> getFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> clearFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    private ContainerState getContainerState(ContainerIdentity identity) {
        return containerRegistry.get().getContainer(identity);
    }

    private ContainerState getRequiredContainer(ContainerIdentity identity) {
        return containerRegistry.get().getRequiredContainer(identity);
    }

    @Reference
    void bindConfigurationManager(ConfigurationManager service) {
        this.configManager.bind(service);
    }
    void unbindConfigurationManager(ConfigurationManager service) {
        this.configManager.unbind(service);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    void bindContainerLifecycleHandler(ContainerCreateHandler service) {
        synchronized (createHandlers) {
            createHandlers.add(service);
        }
    }
    void unbindContainerLifecycleHandler(ContainerCreateHandler service) {
        synchronized (createHandlers) {
            createHandlers.remove(service);
        }
    }

    @Reference
    void bindClusterDataStore(ClusterDataStore service) {
        clusterData.bind(service);
    }
    void unbindClusterDataStore(ClusterDataStore service) {
        clusterData.unbind(service);
    }

    @Reference
    void bindDefaultContainerCreateHandler(DefaultContainerCreateHandler service) {
    }
    void unbindDefaultContainerCreateHandler(DefaultContainerCreateHandler service) {
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }
    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
    }

    @Reference
    void bindEventDispatcher(EventDispatcher service) {
        eventDispatcher.bind(service);
    }
    void unbindEventDispatcher(EventDispatcher service) {
        eventDispatcher.unbind(service);
    }

    @Reference
    void bindPermitManager(PermitManager service) {
        permitManager.bind(service);
    }
    void unbindPermitManager(PermitManager service) {
        permitManager.unbind(service);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }
    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }

    static final class ContainerState {

        private final ContainerState parentState;
        private final ContainerIdentity identity;
        private final AttributeSupport attributes;
        private final Set<String> profiles = new HashSet<>();
        private final List<ContainerHandle> handles = new ArrayList<>();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private ProfileVersionState versionState;
        private State state;

        private ContainerState(ContainerState parentState, ContainerIdentity identity, CreateOptions options, List<ContainerHandle> handles, String configToken) {
            NotNullException.assertValue(identity, "identity");
            NotNullException.assertValue(options, "options");
            NotNullException.assertValue(handles, "handles");
            NotNullException.assertValue(configToken, "configToken");
            this.parentState = parentState;
            this.identity = identity;
            this.state = State.CREATED;
            this.handles.addAll(handles);
            this.attributes = new AttributeSupport(options.getAttributes());
            this.attributes.putAttribute(Container.ATTKEY_CONFIG_TOKEN, configToken);
            if (parentState != null) {
                parentState.addChild(this);
            }
        }

        LockHandle aquireWriteLock() {
            final WriteLock writeLock = readWriteLock.writeLock();

            boolean success;
            try {
                success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                success = false;
            }
            IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + identity);

            final LockHandle versionLock;
            if (versionState != null) {
                try {
                    versionLock = versionState.aquireWriteLock();
                } catch (RuntimeException ex) {
                    writeLock.unlock();
                    throw ex;
                }
            } else {
                versionLock = null;
            }

            return new LockHandle() {
                @Override
                public void unlock() {
                    if (versionLock != null) {
                        versionLock.unlock();
                    }
                    writeLock.unlock();
                }
            };
        }

        LockHandle aquireReadLock() {
            final ReadLock readLock = readWriteLock.readLock();

            boolean success;
            try {
                success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                success = false;
            }
            IllegalStateAssertion.assertTrue(success, "Cannot obtain read lock in time for: " + identity);

            return new LockHandle() {
                @Override
                public void unlock() {
                    readLock.unlock();
                }
            };
        }

        ContainerIdentity getIdentity() {
            return identity;
        }

        State getState() {
            return state;
        }

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }

        Host getHost() {
            throw new UnsupportedOperationException();
        }

        Set<String> getManagementDomains() {
            throw new UnsupportedOperationException();
        }

        ContainerState getParentState() {
            return parentState != null ? parentState : null;
        }

        Set<ContainerIdentity> getChildIdentities() {
            LockHandle readLock = aquireReadLock();
            try {
                return Collections.unmodifiableSet(new HashSet<>(children.keySet()));
            } finally {
                readLock.unlock();
            }
        }

        ProfileVersionState getProfileVersion() {
            LockHandle readLock = aquireReadLock();
            try {
                return versionState;
            } finally {
                readLock.unlock();
            }
        }

        Set<String> getProfileIdentities() {
            LockHandle readLock = aquireReadLock();
            try {
                return Collections.unmodifiableSet(new HashSet<>(profiles));
            } finally {
                readLock.unlock();
            }
        }

        Set<ServiceEndpointIdentity<?>> getServiceEndpointIdentities() {
            return Collections.unmodifiableSet(new HashSet<>(getServiceEndpoints().keySet()));
        }

        @SuppressWarnings("unchecked")
        <T extends ServiceEndpoint> T getServiceEndpoint(Class<T> type) {
            NotNullException.assertValue(type, "type");
            T endpoint = null;
            for (ServiceEndpoint ep : getServiceEndpoints().values()) {
                if (type.isAssignableFrom(ep.getClass())) {
                    if (endpoint == null) {
                        endpoint = (T) ep;
                    } else {
                        LOGGER.warn("Multiple service endpoints of type {} for: {}", type.getName(), identity);
                        endpoint = null;
                        break;
                    }
                }
            }
            return endpoint;
        }

        ServiceEndpoint getServiceEndpoint(ServiceEndpointIdentity<?> identity) {
            return getServiceEndpoints().get(identity);
        }

        Map<ServiceEndpointIdentity<?>, ServiceEndpoint> getServiceEndpoints() {
            Map<ServiceEndpointIdentity<?>, ServiceEndpoint> endpoints = new HashMap<>();
            for (ContainerHandle handle : handles) {
                for (ServiceEndpoint ep : handle.getServiceEndpoints()) {
                    endpoints.put(ep.getIdentity(), ep);
                }
            }
            return Collections.unmodifiableMap(endpoints);
        }

        ImmutableContainer immutableContainer() {
            LockHandle readLock = aquireReadLock();
            try {
                ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, getAttributes(), getState());
                builder.addParent(parentState != null ? parentState.getIdentity() : null);
                builder.addProfileVersion(versionState != null ? versionState.getIdentity() : null);
                builder.addChildren(getChildIdentities());
                builder.addProfiles(getProfileIdentities());
                builder.addServiceEndpoints(getServiceEndpointIdentities());
                return builder.build();
            } finally {
                readLock.unlock();
            }
        }

        private List<ContainerHandle> getContainerHandles() {
            return Collections.unmodifiableList(handles);
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ContainerService} is supposed to mutate the ContainerState

        private void addChild(ContainerState childState) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                children.put(childState.getIdentity(), childState);
            } finally {
                writeLock.unlock();
            }
        }

        private void removeChild(ContainerIdentity identity) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                children.remove(identity);
            } finally {
                writeLock.unlock();
            }
        }

        private void setProfileVersion(ProfileVersionState versionState) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                this.versionState = versionState;
            } finally {
                writeLock.unlock();
            }
        }

        private void addProfiles(Set<String> identities) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                profiles.addAll(identities);
            } finally {
                writeLock.unlock();
            }
        }

        private void removeProfiles(Set<String> identities) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                profiles.removeAll(identities);
            } finally {
                writeLock.unlock();
            }
        }

        private ContainerState start() {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                state = State.STARTED;
                return this;
            } finally {
                writeLock.unlock();
            }
        }

        private ContainerState stop() {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                state = State.STOPPED;
                return this;
            } finally {
                writeLock.unlock();
            }
        }

        private ContainerState destroy() {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                if (parentState != null) {
                    parentState.removeChild(identity);
                }
                state = State.DESTROYED;
                return this;
            } finally {
                writeLock.unlock();
            }
        }

        private void assertNotDestroyed() {
            IllegalStateAssertion.assertFalse(state == State.DESTROYED, "Container already destroyed: " + this);
        }

        @Override
        public String toString() {
            Version profileVersion = versionState != null ? versionState.getIdentity() : null;
            return "Container[id=" + identity + ",state=" + state + ",version=" + profileVersion + "]";
        }
    }
}
