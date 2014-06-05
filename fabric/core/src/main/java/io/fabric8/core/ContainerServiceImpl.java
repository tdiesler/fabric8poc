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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Configuration;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItem.Filter;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.Host;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.RequirementItem;
import io.fabric8.api.ResourceItem;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.core.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.domain.controller.Controller;
import io.fabric8.spi.AbstractCreateOptions;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.RuntimeService;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.ResourceUtils;
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
    @Reference(referenceInterface = Controller.class)
    private final ValidatingReference<Controller> controller = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<>();
    @Reference(referenceInterface = Provisioner.class)
    private final ValidatingReference<Provisioner> provisioner = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();
    private ContainerIdentity currentIdentity;

    @Activate
    void activate(Map<String, ?> config) throws ProvisionException {
        currentIdentity = ContainerIdentity.createFrom(runtimeService.get().getIdentity());
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

                LOGGER.info("Profile updated: {}", profile);

                PermitManager permitManager = ServiceLocator.getRequiredService(PermitManager.class);
                Permit<ContainerService> permit = permitManager.aquirePermit(ContainerService.PERMIT, false);
                try {
                    ContainerServiceImpl service = (ContainerServiceImpl) permit.getInstance();
                    String profileId = profile.getIdentity();
                    for (ContainerState cntState : service.getContainerStates(null)) {
                        List<String> profiles;
                        LockHandle readLock = service.aquireReadLock(cntState.getIdentity());
                        try {
                            profiles = cntState.getProfileIdentities();
                        } finally {
                            readLock.unlock();
                        }
                        if (profiles.contains(profileId)) {
                            LockHandle writeLock = service.aquireContainerLock(cntState.getIdentity());
                            try {
                                service.updateProfileInternal(cntState, profileId, null);
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
        ContainerState currentCnt = containerRegistry.get().getContainer(currentIdentity);
        if (currentCnt == null) {
            CreateOptions options = new AbstractCreateOptions() {
            };
            currentCnt = new ContainerState(null, currentIdentity, options);
            LockHandle writeLock = aquireWriteLock(currentIdentity);
            try {
                currentCnt.addProfiles(new ArrayList<>(bootConfiguration.get().getProfiles()));
                currentCnt.setProfileVersion(((ProfileServiceImpl) profileService.get()).getProfileVersionState(bootConfiguration.get().getVersion()));

                LOGGER.info("Create current container: {}", currentCnt);
                containerRegistry.get().addContainer(currentCnt);

                final CountDownLatch provisionLatch = new CountDownLatch(1);
                ProvisionEventListener provisionListener = new ProvisionEventListener() {
                    @Override
                    public void processEvent(ProvisionEvent event) {
                        provisionLatch.countDown();
                    }
                };

                // Start the current container
                startContainerInternal(currentCnt, provisionListener);

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
    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        assertValid();
        return aquireWriteLock(identity);
    }

    private LockHandle aquireWriteLock(ContainerIdentity identity) {
        return containerLocks.get().aquireWriteLock(identity);
    }

    private LockHandle aquireReadLock(ContainerIdentity identity) {
        return containerLocks.get().aquireReadLock(identity);
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
            ContainerIdentity identity = options.getIdentity();
            ContainerState parentState = parentId != null ? getRequiredContainerState(parentId) : null;
            ContainerState cntState = new ContainerState(parentState, identity, options);
            LOGGER.info("Create container: {}", cntState);
            ContainerRegistry registry = containerRegistry.get();
            if (parentId != null) {
                LockHandle parentLock = aquireWriteLock(parentId);
                try {
                    parentState.addChild(cntState);
                    registry.addContainer(cntState);
                } finally {
                    parentLock.unlock();
                }
            } else {
                registry.addContainer(cntState);
            }
            LockHandle readLock = containerLocks.get().aquireReadLock(identity);
            try {
                return cntState.immutableContainer();
            } finally {
                readLock.unlock();
            }
        }

        return controller.get().createContainer(parentId, options);
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getContainerState(identity);
        if (cntState != null) {
            LockHandle readLock = aquireReadLock(identity);
            try {
                return cntState.immutableContainer();
            } finally {
                readLock.unlock();
            }
        } else {
            return controller.get().getContainer(identity);
        }
    }

    @Override
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return startContainerInternal(cntState, listener);
            } else {
                return controller.get().startContainer(identity, listener);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container startContainerInternal(ContainerState cntState, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Start container: {}", cntState);
        if (cntState.getProfileVersion() == null) {
            Profile defaultProfile = profileService.get().getDefaultProfile();
            setVersionInternal(cntState, defaultProfile.getVersion(), listener);
            addProfilesInternal(cntState, Collections.singletonList(defaultProfile.getIdentity()), listener);
        }

        // Start the container
        cntState.start();

        // Provision the container profiles
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> identities = cntState.getProfileIdentities();
        provisionProfilesInternal(cntState, version, identities, listener);

        return cntState.immutableContainer();
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        assertValid();
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot stop current container");
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return stopContainerInternal(cntState);
            } else {
                return controller.get().stopContainer(identity);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container stopContainerInternal(ContainerState cntState) {
        LOGGER.info("Stop container: {}", cntState);
        return cntState.stop().immutableContainer();
    }

    @Override
    public Container destroyContainer(ContainerIdentity identity) {
        assertValid();
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot destroy current container");
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return destroyContainerInternal(cntState);
            } else {
                return controller.get().destroyContainer(identity);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container destroyContainerInternal(ContainerState cntState) {

        ContainerIdentity identity = cntState.getIdentity();
        Set<ContainerIdentity> childIdentities = cntState.getChildIdentities();
        IllegalStateAssertion.assertTrue(childIdentities.isEmpty(), "Cannot destroy a container that has active child containers: " + cntState);

        // Stop the container
        if (cntState.getState() == State.STARTED) {
            stopContainer(identity);
        }

        LOGGER.info("Destroy container: {}", cntState);
        containerRegistry.get().removeContainer(identity);
        ContainerState parentState = cntState.getParentState();
        if (parentState != null) {
            LockHandle parentLock = aquireWriteLock(parentState.getIdentity());
            try {
                parentState.removeChild(identity);
            } finally {
                parentLock.unlock();
            }
        }
        cntState.destroy();
        return cntState.immutableContainer();
    }

    @Override
    public Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        Set<ContainerIdentity> identities = new HashSet<>(containerRegistry.get().getContainerIdentities());
        identities.addAll(controller.get().getContainerIdentities());
        return Collections.unmodifiableSet(identities);
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<Container> result = new HashSet<>();
        for (ContainerState cntState : getContainerStates(identities)) {
            LockHandle readLock = aquireReadLock(cntState.getIdentity());
            try {
                result.add(cntState.immutableContainer());
            } finally {
                readLock.unlock();
            }
        }
        result.addAll(controller.get().getContainers(identities));
        return Collections.unmodifiableSet(result);
    }

    private Set<ContainerState> getContainerStates(Set<ContainerIdentity> identities) {
        return containerRegistry.get().getContainers(null);
    }

    @Override
    public Container getCurrentContainer() {
        assertValid();
        ContainerState cntState = getRequiredContainerState(currentIdentity);
        LockHandle readLock = aquireReadLock(cntState.getIdentity());
        try {
            return cntState.immutableContainer();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, Version version, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return setVersionInternal(cntState, version, listener);
            } else {
                return controller.get().setProfileVersion(identity, version, listener);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container setVersionInternal(ContainerState cntState, Version nextVersion, ProvisionEventListener listener) throws ProvisionException {

        ProfileVersionState nextVersionState = ((ProfileServiceImpl) profileService.get()).getProfileVersionState(nextVersion);
        LOGGER.info("Set container version: {} <= {}", cntState, nextVersion);

        // Provision the next profiles
        List<String> identities = cntState.getProfileIdentities();
        provisionProfilesInternal(cntState, nextVersion, identities, listener);

        cntState.setProfileVersion(nextVersionState);
        return cntState.immutableContainer();
    }

    @Override
    public Container addProfiles(ContainerIdentity identity, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return addProfilesInternal(cntState, profiles, listener);
            } else {
                return controller.get().addProfiles(identity, profiles, listener);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container addProfilesInternal(ContainerState cntState, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Add container profiles: {} <= {}", cntState, profiles);

        // Provision the profiles
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> effective = new ArrayList<>(cntState.getProfileIdentities());
        effective.addAll(profiles);
        provisionProfilesInternal(cntState, version, effective, listener);

        // Update the references
        cntState.addProfiles(profiles);
        return cntState.immutableContainer();
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        LockHandle writeLock = aquireWriteLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return removeProfilesInternal(cntState, profiles, listener);
            } else {
                return controller.get().removeProfiles(identity, profiles, listener);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Container removeProfilesInternal(ContainerState cntState, List<String> profiles, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Remove container profiles: {} => {}", cntState, profiles);

        // Unprovision the profiles
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> effective = new ArrayList<>(cntState.getProfileIdentities());
        effective.removeAll(profiles);
        provisionProfilesInternal(cntState, version, effective, listener);

        // Update the references
        cntState.removeProfiles(profiles);
        return cntState.immutableContainer();
    }

    @Override
    public Profile getEffectiveProfile(ContainerIdentity identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                Version version = cntState.getProfileVersion().getIdentity();
                List<String> identities = cntState.getProfileIdentities();
                return getEffectiveProfileInternal(cntState, version, identities);
            } else {
                return controller.get().getEffectiveProfile(identity);
            }
        } finally {
            readLock.unlock();
        }
    }

    private Profile getEffectiveProfileInternal(ContainerState cntState, Version version, List<String> identities) {
        LockHandle readLock = aquireReadLock(cntState.getIdentity());
        try {
            StringBuffer effectiveId = new StringBuffer("effective#" + version + "[");
            for (int i = 0; i < identities.size(); i++) {
                effectiveId.append((i > 0 ? "," : "") + identities.get(i));
            }
            effectiveId.append("]");
            ProfileBuilder prfBuilder = new DefaultProfileBuilder(effectiveId.toString());
            prfBuilder.profileVersion(version);
            for (String profileId : identities) {
                LinkedProfile linkedProfile = profileService.get().getLinkedProfile(version, profileId);
                ProfileUtils.buildEffectiveProfile(prfBuilder, linkedProfile);
            }
            return prfBuilder.getProfile();
        } finally {
            readLock.unlock();
        }
    }

    private void updateProfileInternal(ContainerState cntState, String identity, ProvisionEventListener listener) throws ProvisionException {

        LOGGER.info("Update container profile: {} <= {}", cntState, identity);

        // Provision the profile
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> identities = cntState.getProfileIdentities();
        provisionProfilesInternal(cntState, version, identities, listener);
    }

    private void provisionProfilesInternal(ContainerState cntState, Version version, List<String> identities, ProvisionEventListener listener) throws ProvisionException {
        if (cntState.getState() == State.STARTED) {
            Profile effective = getEffectiveProfileInternal(cntState, version, identities);
            provisionEffectiveProfile(cntState, effective, listener);
        }
    }

    private void provisionEffectiveProfile(ContainerState cntState, Profile effective, ProvisionEventListener listener) throws ProvisionException {

        LOGGER.info("Provision profile: {} <= {}", cntState, effective.getIdentity());

        Container container = cntState.immutableContainer();
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
        List<ResourceIdentity> removalPending = new ArrayList<>();
        Map<ResourceIdentity, ResourceHandle> currentResources = cntState.getResourceHandles();
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
        cntState.removeResourceHandles(removalPending);

        // Install added resources
        Map<ResourceIdentity, ResourceHandle> addedResources = new LinkedHashMap<>();
        for (Resource res : allResources.values()) {
            ResourceIdentity resid = res.getIdentity();
            if (currentResources.get(resid) == null) {
                ResourceHandle handle = provisioner.get().installResource(res);
                addedResources.put(resid, handle);
            }
        }
        cntState.addResourceHandles(addedResources);

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
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return cntState.getServiceEndpoint(type);
            } else {
                return controller.get().getServiceEndpoint(identity, type);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity<?> endpointId) {
        LockHandle readLock = aquireReadLock(identity);
        try {
            ContainerState cntState = getContainerState(identity);
            if (cntState != null) {
                return cntState.getServiceEndpoint(endpointId);
            } else {
                return controller.get().getServiceEndpoint(identity, endpointId);
            }
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
        ContainerState cntState = getRequiredContainerState(currentIdentity);
        LockHandle readLock = aquireReadLock(cntState.getIdentity());
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

    private ContainerState getContainerState(ContainerIdentity identity) {
        return containerRegistry.get().getContainer(identity);
    }

    private ContainerState getRequiredContainerState(ContainerIdentity identity) {
        return containerRegistry.get().getRequiredContainer(identity);
    }

    void bindBootConfiguration(BootConfiguration service) {
        bootConfiguration.bind(service);
    }
    void unbindBootConfiguration(BootConfiguration service) {
        bootConfiguration.unbind(service);
    }

    void bindController(Controller service) {
        this.controller.bind(service);
    }
    void unbindController(Controller service) {
        this.controller.unbind(service);
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

    static final class ContainerState {

        private final ContainerState parentState;
        private final ContainerIdentity identity;
        private final AttributeSupport attributes;
        private final List<String> profiles = new ArrayList<>();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<>();
        private final Map<ResourceIdentity, ResourceHandle> resourceHandles = new LinkedHashMap<>();
        private ProfileVersionState versionState;
        private State state;

        private ContainerState(ContainerState parentState, ContainerIdentity identity, CreateOptions options) {
            IllegalArgumentAssertion.assertNotNull(identity, "identity");
            IllegalArgumentAssertion.assertNotNull(options, "options");
            this.parentState = parentState;
            this.identity = identity;
            this.state = State.CREATED;
            this.attributes = new AttributeSupport(options.getAttributes(), true);
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
            assertReadLock();
            return Collections.unmodifiableSet(new HashSet<>(children.keySet()));
        }

        ProfileVersion getProfileVersion() {
            return versionState != null ? versionState.getProfileVersion() : null;
        }

        List<String> getProfileIdentities() {
            assertReadLock();
            return Collections.unmodifiableList(new ArrayList<>(profiles));
        }

        Set<ServiceEndpointIdentity<?>> getServiceEndpointIdentities() {
            return Collections.unmodifiableSet(new HashSet<>(getServiceEndpoints().keySet()));
        }

        @SuppressWarnings("unchecked")
        <T extends ServiceEndpoint> T getServiceEndpoint(Class<T> type) {
            IllegalArgumentAssertion.assertNotNull(type, "type");
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
            return Collections.unmodifiableMap(endpoints);
        }

        ImmutableContainer immutableContainer() {
            assertReadLock();
            ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, getAttributes(), getState());
            builder.addParent(parentState != null ? parentState.getIdentity() : null);
            builder.addProfileVersion(versionState != null ? versionState.getIdentity() : null);
            builder.addChildren(getChildIdentities());
            builder.addProfiles(getProfileIdentities());
            builder.addServiceEndpoints(getServiceEndpointIdentities());
            return builder.build();
        }

        Map<ResourceIdentity, ResourceHandle> getResourceHandles() {
            return Collections.unmodifiableMap(resourceHandles);
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ContainerService} is supposed to mutate the ContainerState

        private void addChild(ContainerState childState) {
            assertNotDestroyed();
            assertWriteLock();
            children.put(childState.getIdentity(), childState);
        }

        private void removeChild(ContainerIdentity identity) {
            assertNotDestroyed();
            assertWriteLock();
            children.remove(identity);
        }

        private void setProfileVersion(ProfileVersionState versionState) {
            assertNotDestroyed();
            assertWriteLock();
            this.versionState = versionState;
        }

        private void addProfiles(List<String> identities) {
            assertNotDestroyed();
            assertWriteLock();
            profiles.addAll(identities);
        }

        private void removeProfiles(List<String> identities) {
            assertNotDestroyed();
            assertWriteLock();
            profiles.removeAll(identities);
        }

        private ContainerState start() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.STARTED;
            return this;
        }

        private ContainerState stop() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.STOPPED;
            return this;
        }

        private ContainerState destroy() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.DESTROYED;
            return this;
        }

        private void addResourceHandles(Map<ResourceIdentity, ResourceHandle> handles) {
            assertNotDestroyed();
            assertWriteLock();
            resourceHandles.putAll(handles);
        }

        private void removeResourceHandles(Collection<ResourceIdentity> handles) {
            assertNotDestroyed();
            assertWriteLock();
            for (ResourceIdentity resid : handles) {
                resourceHandles.remove(resid);
            }
        }

        private void assertNotDestroyed() {
            IllegalStateAssertion.assertFalse(state == State.DESTROYED, "Container already destroyed: " + this);
        }

        private void assertReadLock() {
            ContainerLockManager.assertReadLock(identity);
        }

        private void assertWriteLock() {
            ContainerLockManager.assertWriteLock(identity);
        }

        @Override
        public String toString() {
            Version profileVersion = versionState != null ? versionState.getIdentity() : null;
            return "ContainerState[id=" + identity + ",state=" + state + ",version=" + profileVersion + "]";
        }
    }
}
