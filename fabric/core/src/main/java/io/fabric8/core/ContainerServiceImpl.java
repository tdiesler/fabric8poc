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

import io.fabric8.api.Configuration;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItem.Filter;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.RequirementItem;
import io.fabric8.api.ResourceItem;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.ContainerRegistration;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.ServiceEndpointFacotryLocator;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.ProfileUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.ProvisionException;
import org.jboss.gravia.provision.ProvisionResult;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.resolver.Environment;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The internal {@link ContainerService}
 *
 * It is the owner of all {@link Container} instances.
 * All mutating operations must go through this service.
 * The public API returns shallow immutable {@link Container} instances, mutable {@link Container} instances must not leak outside this service.
 *
 * This service is protected by a permit.
 * Access without the calling client holding a permit is considered a programming error.
 *
 * Concurrency & Locking Strategy
 * ------------------------------
 *
 * Read access to {@link Container} can happen concurrently.
 * Each {@link Container} instance is associated with a {@link ReentrantReadWriteLock}
 *
 * A client may explicitly acquire a write lock for a {@link Container}.
 * Obtaining a write lock for a {@link Container} also obtains a write lock on the associated {@link ProfileVersion}.
 * This is necessary when exclusive access to {@link Container} content is required. For example when making multiple calls
 * as part of one operation - while doing so the {@link Container} and its associated {@link ProfileVersion} must be locked and cannot change.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(configurationPid = Container.CONTAINER_SERVICE_PID, policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(ContainerService.class)
@References({ @Reference(referenceInterface = EventDispatcher.class), @Reference(referenceInterface = PermitManager.class) })
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerServiceImpl.class);

    @Reference(referenceInterface = BootConfiguration.class)
    private final ValidatingReference<BootConfiguration> bootConfiguration = new ValidatingReference<>();
    @Reference(referenceInterface = ConfigurationManager.class)
    private final ValidatingReference<ConfigurationManager> configurationManager = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerLockManager.class)
    private final ValidatingReference<ContainerLockManager> containerLocks = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerRegistration.class)
    private final ValidatingReference<ContainerRegistration> containerRegistration = new ValidatingReference<>();
    @Reference(referenceInterface = CurrentContainerService.class)
    private final ValidatingReference<CurrentContainerService> currentContainerService = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<>();
    @Reference(referenceInterface = Provisioner.class)
    private final ValidatingReference<Provisioner> provisioner = new ValidatingReference<>();
    @Reference(referenceInterface = RemoteContainerService.class)
    private final ValidatingReference<RemoteContainerService> remoteContainer = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();
    private ContainerIdentity currentIdentity;

    @Activate
    void activate(Map<String, ?> config) throws ProvisionException {
        currentIdentity = ContainerIdentity.createFrom(runtimeService.get().getRuntimeIdentity());
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

    private void activateInternal() throws ProvisionException {

        // Register a listener for profile update events
        ProfileEventListener listener = new ProfileEventListener() {

            @Override
            public void processEvent(ProfileEvent event) {
                Profile profile = event.getSource();
                if (event.getType() != ProfileEvent.EventType.UPDATED)
                    return;

                LOGGER.info("Update profile: {}", profile);
                ProfileIdentity prfid = profile.getIdentity();

                PermitManager permitManager = ServiceLocator.getRequiredService(PermitManager.class);
                Permit<ContainerService> permit = permitManager.aquirePermit(ContainerService.PERMIT, false);
                try {
                    ContainerService service = permit.getInstance();
                    for (Container cnt : service.getContainers(null)) {
                        List<ProfileIdentity> profiles = cnt.getProfileIdentities();
                        if (profiles.contains(prfid)) {
                            ContainerIdentity cntid = cnt.getIdentity();
                            LockHandle writeLock = service.aquireContainerLock(cntid);
                            try {
                                service.updateProfile(cntid, prfid, null);
                            } catch (ProvisionException ex) {
                                LOGGER.error("Cannot update container profile: " + profile, ex);
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

        // Create the current container
        ContainerRegistry registry = containerRegistry.get();
        Container container = registry.getRequiredContainer(currentIdentity);

        LockHandle writeLock = aquireWriteLock(currentIdentity);
        try {
            final CountDownLatch provisionLatch = new CountDownLatch(1);
            ProvisionEventListener provisionListener = new ProvisionEventListener() {
                @Override
                public void processEvent(ProvisionEvent event) {
                    provisionLatch.countDown();
                }
            };

            // Start the current container
            startContainerInternal(container, provisionListener);

            try {
                boolean success = provisionLatch.await(10, TimeUnit.SECONDS);
                IllegalStateAssertion.assertTrue(success, "Cannot provision current container");
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        assertValid();
        return aquireWriteLock(identity);
    }

    private LockHandle aquireWriteLock(ContainerIdentity identity) {
        final AtomicReference<LockHandle> verLock = new AtomicReference<>();
        final LockHandle cntLock = containerLocks.get().aquireWriteLock(identity);
        try {
            verLock.set(profileService.get().aquireReadLock());
        } catch (RuntimeException rte) {
            safeUnlock(cntLock, verLock);
            throw rte;
        } catch (Error err) {
            safeUnlock(cntLock, verLock);
            throw err;
        }
        return new LockHandle() {
            @Override
            public void unlock() {
                safeUnlock(cntLock, verLock);
            }
        };
    }

    private LockHandle aquireReadLock(ContainerIdentity identity) {
        final AtomicReference<LockHandle> verLock = new AtomicReference<>();
        final LockHandle cntLock = containerLocks.get().aquireReadLock(identity);
        try {
            verLock.set(profileService.get().aquireReadLock());
        } catch (RuntimeException rte) {
            safeUnlock(cntLock, verLock);
            throw rte;
        } catch (Error err) {
            safeUnlock(cntLock, verLock);
            throw err;
        }
        return new LockHandle() {
            @Override
            public void unlock() {
                safeUnlock(cntLock, verLock);
            }
        };
    }

    private void safeUnlock(LockHandle cntLock, AtomicReference<LockHandle> lockRef) {
        LockHandle verLock = lockRef.get();
        if (verLock != null) {
            verLock.unlock();
        }
        cntLock.unlock();
    }

    @Override
    public Container createContainer(CreateOptions options) {
        assertValid();
        return createContainerInternal(null, options);
    }

    @Override
    public Container createContainer(ContainerIdentity parentId, CreateOptions options) {
        assertValid();
        return createContainerInternal(parentId, options);
    }

    private Container createContainerInternal(ContainerIdentity parentId, CreateOptions options) {

        // Support for embedded container testing
        if (options.getClass().getName().endsWith("EmbeddedCreateOptions")) {
            Container container;
            ContainerIdentity identity = options.getIdentity();
            LockHandle readLock = aquireReadLock(identity);
            try {
                ContainerRegistry registry = containerRegistry.get();
                container = registry.createContainer(parentId, identity, options, options.getProfileVersion(), options.getProfiles(), new LinkedHashSet<ServiceEndpoint>());
                LOGGER.info("Create container: {}", container);

                // Update parent/child association
                if (parentId != null) {
                    LockHandle parentLock = aquireWriteLock(parentId);
                    try {
                        registry.addChildToParent(parentId, container.getIdentity());
                    } finally {
                        parentLock.unlock();
                    }
                }
            } finally {
                readLock.unlock();
            }
            return container;
        }

        return remoteContainer.get().createContainer(parentId, options);
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(identity);
        try {
            return containerRegistry.get().getContainer(identity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            if (hasContainer(identity)) {
                Container container = getContainer(identity);
                return startContainerInternal(container, listener);
            } else {
                return remoteContainer.get().startContainer(identity, listener);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container startContainerInternal(Container container, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Start container: {}", container);
        if (container.getProfileVersion() == null) {
            Profile defaultProfile = profileService.get().getDefaultProfile();
            setVersionInternal(container, defaultProfile.getVersion(), listener);
            addProfilesInternal(container, Collections.singletonList(defaultProfile.getIdentity()), listener);
        }

        // Start the container
        ContainerRegistry registry = containerRegistry.get();
        container = registry.startContainer(container.getIdentity());

        // Provision the container profiles
        VersionIdentity version = container.getProfileVersion();
        List<ProfileIdentity> identities = container.getProfileIdentities();
        provisionProfilesInternal(container, version, identities, listener);

        return container;
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        assertValid();
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot stop current container");
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            Container container = getContainer(identity);
            if (container != null) {
                return stopContainerInternal(container);
            } else {
                return remoteContainer.get().stopContainer(identity);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container stopContainerInternal(Container container) {
        LOGGER.info("Stop container: {}", container);
        ContainerRegistry registry = containerRegistry.get();
        return registry.stopContainer(container.getIdentity());
    }

    @Override
    public Container destroyContainer(ContainerIdentity identity) {
        assertValid();
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot destroy current container");
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            Container container = getContainer(identity);
            if (container != null) {
                return destroyContainerInternal(container);
            } else {
                return remoteContainer.get().destroyContainer(identity);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container destroyContainerInternal(Container container) {

        ContainerIdentity identity = container.getIdentity();
        Set<ContainerIdentity> childIdentities = container.getChildIdentities();
        IllegalStateAssertion.assertTrue(childIdentities.isEmpty(), "Cannot destroy a container that has active child containers: " + container);

        // Stop the container
        if (container.getState() == State.STARTED) {
            container = stopContainer(identity);
        }

        LOGGER.info("Destroy container: {}", container);

        ContainerRegistry registry = containerRegistry.get();
        ContainerIdentity parentId = container.getParentIdentity();
        if (parentId != null) {
            LockHandle parentLock = aquireWriteLock(parentId);
            try {
                registry.removeChildFromParent(parentId, identity);
            } finally {
                parentLock.unlock();
            }
        }
        return registry.destroyContainer(container.getIdentity());
    }

    @Override
    public Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        ContainerRegistry registry = containerRegistry.get();
        return registry.getContainerIdentities();
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<Container> result = new HashSet<>();
        ContainerRegistry registry = containerRegistry.get();
        for (ContainerIdentity identity : registry.getContainerIdentities()) {
            LockHandle readLock = aquireReadLock(identity);
            try {
                result.add(registry.getContainer(identity));
            } finally {
                readLock.unlock();
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Container getCurrentContainer() {
        assertValid();
        LockHandle readLock = aquireReadLock(currentIdentity);
        try {
            return getRequiredContainer(currentIdentity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, VersionIdentity version, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            Container container = getRequiredContainer(identity);
            return setVersionInternal(container, version, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container setVersionInternal(Container container, VersionIdentity nextVersion, ProvisionEventListener listener) throws ProvisionException {

        LOGGER.info("Set container version: {} <= {}", container, nextVersion);

        // Provision the next profiles
        List<ProfileIdentity> identities = container.getProfileIdentities();
        provisionProfilesInternal(container, nextVersion, identities, listener);

        ContainerRegistry registry = containerRegistry.get();
        return registry.setProfileVersion(container.getIdentity(), nextVersion);
    }

    @Override
    public Container addProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            Container container = getRequiredContainer(identity);
            return addProfilesInternal(container, profiles, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container addProfilesInternal(Container container, List<ProfileIdentity> profiles, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Add container profiles: {} <= {}", container, profiles);

        // Provision the profiles
        VersionIdentity version = container.getProfileVersion();
        List<ProfileIdentity> effective = new ArrayList<>(container.getProfileIdentities());
        effective.addAll(profiles);
        provisionProfilesInternal(container, version, effective, listener);

        // Update the references
        ContainerRegistry registry = containerRegistry.get();
        return registry.addProfiles(container.getIdentity(), profiles);
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            Container container = getRequiredContainer(identity);
            return removeProfilesInternal(container, profiles, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container removeProfilesInternal(Container container, List<ProfileIdentity> profiles, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Remove container profiles: {} => {}", container, profiles);

        // Unprovision the profiles
        VersionIdentity version = container.getProfileVersion();
        List<ProfileIdentity> effective = new ArrayList<>(container.getProfileIdentities());
        effective.removeAll(profiles);
        provisionProfilesInternal(container, version, effective, listener);

        // Update the references
        ContainerRegistry registry = containerRegistry.get();
        return registry.removeProfiles(container.getIdentity(), profiles);
    }

    @Override
    public Profile getEffectiveProfile(ContainerIdentity identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(identity);
        try {
            Container container = getRequiredContainer(identity);
            VersionIdentity version = container.getProfileVersion();
            List<ProfileIdentity> identities = container.getProfileIdentities();
            return getEffectiveProfileInternal(container, version, identities);
        } finally {
            readLock.unlock();
        }
    }

    private Profile getEffectiveProfileInternal(Container container, VersionIdentity version, List<ProfileIdentity> profiles) {
        LockHandle readLock = aquireReadLock(container.getIdentity());
        try {
            StringBuffer effectiveId = new StringBuffer("effective#" + version);
            for (ProfileIdentity prfid : profiles) {
                effectiveId.append("-" + prfid.getSymbolicName());
            }
            ProfileBuilder prfBuilder = new DefaultProfileBuilder(effectiveId.toString());
            prfBuilder.profileVersion(version);
            for (ProfileIdentity profileId : profiles) {
                LinkedProfile linkedProfile = profileService.get().getLinkedProfile(version, profileId);
                ProfileUtils.buildEffectiveProfile(prfBuilder, linkedProfile);
            }
            return prfBuilder.getProfile();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void updateProfile(ContainerIdentity identity, ProfileIdentity profile, ProvisionEventListener listener) throws ProvisionException {

        Container container = getRequiredContainer(identity);
        LOGGER.info("Update container profile: {} <= {}", container, identity);

        // Provision the profile
        VersionIdentity version = container.getProfileVersion();
        List<ProfileIdentity> identities = container.getProfileIdentities();
        provisionProfilesInternal(container, version, identities, listener);
    }

    private void provisionProfilesInternal(Container container, VersionIdentity version, List<ProfileIdentity> identities, ProvisionEventListener listener) throws ProvisionException {
        if (container.getState() == State.STARTED) {
            Profile effective = getEffectiveProfileInternal(container, version, identities);
            provisionEffectiveProfile(container, effective, listener);
        }
    }

    private void provisionEffectiveProfile(Container container, Profile effective, ProvisionEventListener listener) throws ProvisionException {

        LOGGER.info("Provision profile: {} <= {}", container, effective.getIdentity());

        ProvisionEvent event = new ProvisionEvent(container, EventType.PROVISIONING, effective);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);

        // Apply the configuration items
        Filter filter = new ConfigurationItem.Filter() {
            @Override
            public boolean accept(Configuration config) {
                String includedTypes = config.getDirective(ContentNamespace.CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE);
                String excludedTypes = config.getDirective(ContentNamespace.CAPABILITY_EXCLUDE_RUNTIME_TYPE_DIRECTIVE);
                return isRuntimeRelevant(includedTypes, excludedTypes);
            }

        };
        for (ConfigurationItem item : effective.getProfileItems(ConfigurationItem.class)) {
            for (Configuration config : item.getConfigurations(filter)) {
                Map<String, Object> atts = config.getAttributes();
                configurationManager.get().applyConfiguration(item.getIdentity(), atts);
            }
        }

        // Clone the runtime environment & add the explicit {@link ResourceItem}s
        Map<ResourceIdentity, ResourceItem> explicitResources = new LinkedHashMap<>();
        Environment envclone = provisioner.get().getEnvironment().cloneEnvironment();
        for (ResourceItem item : effective.getProfileItems(ResourceItem.class)) {
            Resource res = item.getResource();
            explicitResources.put(res.getIdentity(), item);
            envclone.addResource(res);
        }

        // Get the complete set of requirements
        Set<Requirement> reqs = new HashSet<>();
        for (RequirementItem item : effective.getProfileItems(RequirementItem.class)) {
            Requirement req = item.getRequirement();
            reqs.add(req);
        }

        // Resolve all requirements
        ProvisionResult result = provisioner.get().findResources(envclone, reqs);
        Set<Requirement> unsatisfied = result.getUnsatisfiedRequirements();
        if (!unsatisfied.isEmpty()) {
            throw new ProvisionException("Cannot resolve unsatisfied requirements: " + unsatisfied);
        }

        // Get map of all provisoned resources
        Map<ResourceIdentity, Resource> allResources = new LinkedHashMap<>();
        for (Resource res : result.getResources()) {
            allResources.put(res.getIdentity(), res);
        }
        for (ResourceItem item : explicitResources.values()) {
            Resource res = item.getResource();
            if (!ResourceUtils.isAbstract(res)) {
                allResources.put(res.getIdentity(), res);
            }
        }

        // Get list of resources for removal
        CurrentContainerService resourceHolder = currentContainerService.get();
        List<ResourceIdentity> removalPending = new ArrayList<>();
        Map<ResourceIdentity, ResourceHandle> currentResources = resourceHolder.getResourceHandles();
        for (ResourceIdentity resid : currentResources.keySet()) {
            if (allResources.get(resid) == null) {
                removalPending.add(resid);
            }
        }

        // Uninstall removed resources
        Collections.reverse(removalPending);
        for (ResourceIdentity resid : removalPending) {
            ResourceHandle handle = currentResources.get(resid);
            handle.uninstall();
        }
        resourceHolder.removeResourceHandles(removalPending);

        // Install added resources
        Map<ResourceIdentity, ResourceHandle> addedResources = new LinkedHashMap<>();
        for (Resource res : allResources.values()) {
            ResourceIdentity resid = res.getIdentity();
            if (currentResources.get(resid) == null) {
                ResourceHandle handle = provisioner.get().installResource(res);
                addedResources.put(resid, handle);
            }
        }
        resourceHolder.addResourceHandles(addedResources);

        event = new ProvisionEvent(container, EventType.PROVISIONED, effective);
        eventDispatcher.get().dispatchProvisionEvent(event, listener);
    }

    private boolean isRuntimeRelevant(String includedTypes, String excludedTypes) {
        boolean result = true;
        if (includedTypes != null || excludedTypes != null) {
            Set<RuntimeType> types = new HashSet<>();
            if (includedTypes == null) {
                types.add(RuntimeType.getRuntimeType());
            }

            // Add all included runtime types
            types.addAll(getRuntimeTypes(includedTypes));

            // Remove all excluded runtime types
            types.removeAll(getRuntimeTypes(excludedTypes));

            // Relevant when the current runtime type is included
            result = types.contains(RuntimeType.getRuntimeType());
        }
        return result;
    }

    private Set<RuntimeType> getRuntimeTypes(String directive) {
        Set<RuntimeType> types = new HashSet<>();
        if (directive != null) {
            for (String typespec : directive.split(",")) {
                types.add(RuntimeType.valueOf(typespec.toUpperCase()));
            }
        }
        return types;
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
        LockHandle readLock = aquireReadLock(identity);
        try {
            ContainerRegistry registry = containerRegistry.get();
            return registry.getServiceEndpoint(identity, type);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity endpointId, Class<T> type) {
        LockHandle readLock = aquireReadLock(identity);
        try {
            ContainerRegistry registry = containerRegistry.get();
            return registry.getServiceEndpoint(identity, endpointId);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Failure> getFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> clearFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URLConnection getContainerURLConnection(URL url) throws IOException {
        assertValid();
        String symbolicName = url.getHost();
        Version resVersion = null;
        if (url.getQuery() != null) {
            String query = url.getQuery().substring(1);
            for (String param : query.split("&")) {
                String[] keyval = param.split("=");
                IllegalStateAssertion.assertEquals(2, keyval.length, "Unexpected array length: " + Arrays.asList(keyval));
                String key = keyval[0];
                String val = keyval[1];
                if ("version".equals(key)) {
                    resVersion = Version.parseVersion(val);
                }
            }
        }
        ResourceIdentity explicitId = resVersion != null ? ResourceIdentity.create(symbolicName, resVersion) : null;
        LockHandle readLock = aquireReadLock(currentIdentity);
        try {
            Resource resource = null;
            Version highestVersion = Version.emptyVersion;
            Profile effective = getEffectiveProfile(currentIdentity);
            for (ResourceItem item : effective.getProfileItems(ResourceItem.class)) {
                ResourceIdentity resid = item.getResource().getIdentity();
                if (resid.equals(explicitId)) {
                    resource = item.getResource();
                    break;
                }
                if (resid.getSymbolicName().equals(symbolicName)) {
                    if (resource == null || resid.getVersion().compareTo(highestVersion) > 1) {
                        resource = item.getResource();
                        highestVersion = resid.getVersion();
                    }
                }
            }
            IllegalStateAssertion.assertNotNull(resource, "Cannot find resource for: " + url);
            List<Capability> ccaps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
            IllegalStateAssertion.assertFalse(ccaps.isEmpty(), "Cannot find content capability in: " + resource);
            if (ccaps.size() > 1) {
                LOGGER.warn("Multiple content capabilities in: " + resource);
            }
            ContentCapability ccap = ccaps.get(0).adapt(ContentCapability.class);
            URL contentURL = ccap.getContentURL();
            IllegalStateAssertion.assertNotNull(contentURL, "Cannot obtain content URL from: " + ccap);
            return contentURL.openConnection();
        } finally {
            readLock.unlock();
        }
    }

    private boolean hasContainer(ContainerIdentity identity) {
        return containerRegistry.get().hasContainer(identity);
    }

    private Container getRequiredContainer(ContainerIdentity identity) {
        return containerRegistry.get().getRequiredContainer(identity);
    }

    void bindBootConfiguration(BootConfiguration service) {
        bootConfiguration.bind(service);
    }
    void unbindBootConfiguration(BootConfiguration service) {
        bootConfiguration.unbind(service);
    }

    void bindRemoteContainer(RemoteContainerService service) {
        this.remoteContainer.bind(service);
    }
    void unbindRemoteContainer(RemoteContainerService service) {
        this.remoteContainer.unbind(service);
    }

    void bindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.bind(service);
    }
    void unbindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.unbind(service);
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

    void bindContainerRegistration(ContainerRegistration service) {
        containerRegistration.bind(service);
    }
    void unbindContainerRegistration(ContainerRegistration service) {
        containerRegistration.unbind(service);
    }

    void bindCurrentContainerService(CurrentContainerService service) {
        currentContainerService.bind(service);
    }
    void unbindCurrentContainerService(CurrentContainerService service) {
        currentContainerService.unbind(service);
    }

    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }
    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }

    void bindProvisioner(Provisioner service) {
        provisioner.bind(service);
    }
    void unbindProvisioner(Provisioner service) {
        provisioner.unbind(service);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }
}
