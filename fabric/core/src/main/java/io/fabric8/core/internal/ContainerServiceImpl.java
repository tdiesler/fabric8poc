/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.Host;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.Container.State;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(service = { ContainerService.class }, configurationPid = Container.CONTAINER_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerServiceImpl.class);

    private final ValidatingReference<ConfigurationManager> configManager = new ValidatingReference<ConfigurationManager>();
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

    private String configToken;
    private ServiceRegistration<?> listenerRegistration;

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
        if (listenerRegistration != null) {
            listenerRegistration.unregister();
        }
    }

    private void activateInternal() {

        // Register a listener for profile update events
        ProfileEventListener listener = new ProfileEventListener() {

            @Override
            public void processEvent(ProfileEvent event) {
                Profile profile = event.getSource();
                Set<ContainerIdentity> identities = profile.getContainers();

                LOGGER.info("Profile updated: {} => {}", profile, identities);
                if (identities.isEmpty() || event.getType() != ProfileEvent.EventType.UPDATED)
                    return;

                Permit<ContainerService> permit = permitManager.get().aquirePermit(ContainerService.PERMIT, false);
                try {
                    ContainerServiceImpl service = (ContainerServiceImpl) permit.getInstance();
                    ProfileIdentity profileId = profile.getIdentity();
                    for (ContainerIdentity identity : identities) {
                        ContainerState cntState = service.getContainerState(identity);
                        if (cntState != null && cntState.getProfiles().contains(profileId)) {
                            LockHandle writeLock = aquireWriteLock(identity);
                            try {
                                service.updateProfileInternal(cntState, profile, null);
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
        listenerRegistration = syscontext.registerService(ProfileEventListener.class, listener, null);
    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        assertValid();
        return aquireWriteLock(identity);
    }

    private LockHandle aquireWriteLock(ContainerIdentity identity) {
        return containerRegistry.get().aquireWriteLock(identity);
    }

    private LockHandle aquireReadLock(ContainerIdentity identity) {
        return containerRegistry.get().aquireReadLock(identity);
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
        ContainerState cntState = new ContainerState(parentState, options, configToken);
        LOGGER.info("Create container: {}", cntState);
        containerRegistry.get().addContainer(parentState, cntState);
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();

        LockHandle readLock = null;
        ContainerState cntState;
        synchronized (containerRegistry) {
            cntState = containerRegistry.get().getContainer(identity);
            if (cntState != null) {
                readLock = aquireReadLock(identity);
            }
        }
        if (cntState == null)
            return null;

        try {
            return new ImmutableContainer(cntState);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            LOGGER.info("Start container: {}", cntState);
            Profile defaultProfile = profileService.get().getDefaultProfile();
            setVersionInternal(cntState, defaultProfile.getProfileVersion(), listener);
            addProfilesInternal(cntState, Collections.singleton(defaultProfile.getIdentity()), listener);
            return new ImmutableContainer(cntState.start());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            LOGGER.info("Stop container: {}", cntState);
            return new ImmutableContainer(cntState.stop());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Container destroyContainer(ContainerIdentity identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            if (!cntState.getChildContainers().isEmpty())
                throw new IllegalStateException("Cannot destroy a container that has active child containers: " + identity);

            // Unprovision the associated profiles
            Version profileVersion = cntState.getProfileVersion();
            if (profileVersion != null) {
                Set<ProfileIdentity> profiles = cntState.getProfiles();
                for (Profile profile : profileService.get().getProfiles(profileVersion, profiles)) {
                    unprovisionProfile(cntState, profile, null);
                }
            }
            synchronized (containerRegistry) {
                LOGGER.info("Destroy container: {}", cntState);
                containerRegistry.get().removeContainer(identity);
                cntState.destroy();
            }
            return new ImmutableContainer(cntState);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<ContainerIdentity> getContainerIds() {
        assertValid();
        return containerRegistry.get().getContainerIds();
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<Container> result = new HashSet<Container>();
        for (ContainerState aux : containerRegistry.get().getContainers(identities)) {
            if (identities == null || identities.contains(aux.getIdentity())) {
                LockHandle readLock = aquireReadLock(aux.getIdentity());
                try {
                    result.add(new ImmutableContainer(aux));
                } finally {
                    readLock.unlock();
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Container getCurrentContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, Version version, ProvisionEventListener listener) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            return setVersionInternal(cntState, version, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container setVersionInternal(ContainerState cntState, Version nextVersion, ProvisionEventListener listener) {

        Version prevVersion = cntState.getProfileVersion();
        Set<ProfileIdentity> cntProfiles = cntState.getProfiles();
        Set<Profile> nextProfiles = profileService.get().getProfiles(nextVersion, cntProfiles);

        LOGGER.info("Set container version: {} <= {}", cntState, nextVersion);

        // Unprovision the previous profiles
        if (prevVersion != null) {
            Set<Profile> prevProfiles = profileService.get().getProfiles(prevVersion, cntProfiles);
            unprovisionProfiles(cntState, prevProfiles, listener);
            profileService.get().removeContainerFromProfileVersion(prevVersion, cntState.getIdentity());
        }

        // Provision the next profiles
        provisionProfiles(cntState, nextProfiles, listener);

        // Update the references
        cntState.setProfileVersion(nextVersion);
        profileService.get().addContainerToProfileVersion(nextVersion, cntState.getIdentity());
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container addProfiles(ContainerIdentity identity, Set<ProfileIdentity> identities, ProvisionEventListener listener) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            return addProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container addProfilesInternal(ContainerState cntState, Set<ProfileIdentity> identities, ProvisionEventListener listener) {
        Version version = cntState.getProfileVersion();
        Set<Profile> profiles = profileService.get().getProfiles(version, identities);

        LOGGER.info("Add container profiles: {} <= {}", cntState, identities);

        // Provision the profiles
        provisionProfiles(cntState, profiles, listener);

        // Update the references
        cntState.addProfiles(identities);
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, Set<ProfileIdentity> identities, ProvisionEventListener listener) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getRequiredContainer(identity);
            return removeProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container removeProfilesInternal(ContainerState cntState, Set<ProfileIdentity> identities, ProvisionEventListener listener) {
        Version version = cntState.getProfileVersion();
        Set<Profile> profiles = profileService.get().getProfiles(version, identities);

        LOGGER.info("Remove container profiles: {} => {}", cntState, identities);

        // Unprovision the profiles
        unprovisionProfiles(cntState, profiles, listener);

        // Update the references
        cntState.removeProfiles(identities);
        return new ImmutableContainer(cntState);
    }

    private void updateProfileInternal(ContainerState cntState, Profile profile, ProvisionEventListener listener) {

        LOGGER.info("Update container profile: {} <= {}", cntState, profile);

        // Unprovision the profile
        unprovisionProfile(cntState, profile, listener);

        // Provision the profile
        provisionProfile(cntState, profile, listener);
    }

    private void provisionProfiles(ContainerState cntState, Set<Profile> profiles, ProvisionEventListener listener) {
        for (Profile profile : profiles) {
            provisionProfile(cntState, profile, listener);
        }
    }

    private void provisionProfile(ContainerState cntState, Profile profile, ProvisionEventListener listener) {
        LOGGER.info("Provision profile: {} <= {}", cntState, profile);

        Container container = new ImmutableContainer(cntState);
        ProvisionEvent event = new ProvisionEvent(container, EventType.PROVISIONING, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);

        // Do the provisioning
        Set<ConfigurationProfileItem> configItems = profile.getProfileItems(ConfigurationProfileItem.class);
        configManager.get().applyConfigurationItems(configItems);

        // Associate the profile with the container
        profileService.get().addContainerToProfile(cntState.getProfileVersion(), profile.getIdentity(), cntState.getIdentity());

        event = new ProvisionEvent(container, EventType.PROVISIONED, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);
    }

    private void unprovisionProfiles(ContainerState cntState, Set<Profile> profiles, ProvisionEventListener listener) {
        for (Profile profile : profiles) {
            unprovisionProfile(cntState, profile, listener);
        }
    }

    private void unprovisionProfile(ContainerState cntState, Profile profile, ProvisionEventListener listener) {
        LOGGER.info("Unprovision profile: {} => {}", cntState, profile);

        Container container = new ImmutableContainer(cntState);
        ProvisionEvent event = new ProvisionEvent(container, EventType.REMOVING, profile);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);

        // do the removing

        // Unassociate the profile with the container
        profileService.get().removeContainerFromProfile(cntState.getProfileVersion(), profile.getIdentity(), cntState.getIdentity());

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

        private final ContainerState parent;
        private final ContainerIdentity identity;
        private final AttributeSupport attributes;
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<ContainerIdentity, ContainerState>();
        private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
        private Version profileVersion;
        private State state;

        ContainerState(ContainerState parent, CreateOptions options, String configToken) {
            this.parent = parent;
            this.state = State.CREATED;
            String parentName = parent != null ? parent.getIdentity().getSymbolicName() + ":" : "";
            this.identity = ContainerIdentity.create(parentName + options.getSymbolicName());
            this.attributes = new AttributeSupport(options.getAttributes());
            this.attributes.putAttribute(Container.ATTKEY_CONFIG_TOKEN, configToken);
        }

        ContainerIdentity getIdentity() {
            return identity;
        }

        State getState() {
            return state;
        }

        Version getProfileVersion() {
            return profileVersion;
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

        ContainerState getParent() {
            return parent != null ? parent : null;
        }

        Set<ContainerIdentity> getChildContainers() {
            return Collections.unmodifiableSet(new HashSet<ContainerIdentity>(children.keySet()));
        }

        Set<String> getManagementDomains() {
            throw new UnsupportedOperationException();
        }

        Set<ServiceEndpointIdentity> getServiceEndpoints() {
            throw new UnsupportedOperationException();
        }

        Set<ProfileIdentity> getProfiles() {
            return Collections.unmodifiableSet(profiles);
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ContainerService} is supposed to mutate the ContainerState

        // Package protected. Adding/Removing a container and setting the parent/child relationship is an atomic operation
        void addChild(ContainerState childState) {
            assertNotDestroyed();
            children.put(childState.getIdentity(), childState);
        }

        // Package protected. Adding/Removing a container and setting the parent/child relationship is an atomic operation
        void removeChild(ContainerIdentity identity) {
            assertNotDestroyed();
            children.remove(identity);
        }

        private void setProfileVersion(Version version) {
            assertNotDestroyed();
            profileVersion = version;
        }

        private void addProfiles(Set<ProfileIdentity> identities) {
            assertNotDestroyed();
            profiles.addAll(identities);
        }

        private void removeProfiles(Set<ProfileIdentity> identities) {
            assertNotDestroyed();
            profiles.removeAll(identities);
        }

        private ContainerState start() {
            assertNotDestroyed();
            state = State.STARTED;
            return this;
        }

        private ContainerState stop() {
            assertNotDestroyed();
            state = State.STOPPED;
            return this;
        }

        private ContainerState destroy() {
            assertNotDestroyed();
            state = State.DESTROYED;
            return this;
        }

        private void assertNotDestroyed() {
            if (state == State.DESTROYED)
                throw new IllegalStateException("Container already destroyed: " + this);
        }

        @Override
        public String toString() {
            return "Container[id=" + identity + ",state=" + state + ",version=" + profileVersion + "]";
        }
    }
}
