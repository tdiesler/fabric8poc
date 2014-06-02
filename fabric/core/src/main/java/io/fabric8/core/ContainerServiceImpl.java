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
import io.fabric8.core.zookeeper.locks.Lock;
import io.fabric8.core.zookeeper.locks.ReadWriteLock;
import io.fabric8.core.zookeeper.locks.ZooKeeperLockManager;
import io.fabric8.spi.AbstractCreateOptions;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.ContainerHandle;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.ManagedCreateOptions;
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

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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

    @Reference(referenceInterface = ConfigurationManager.class)
    private final ValidatingReference<ConfigurationManager> configurationManager = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<>();
    @Reference(referenceInterface = Provisioner.class)
    private final ValidatingReference<Provisioner> provisioner = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    @Reference(referenceInterface = ContainerCreateHandler.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
    private final Set<ContainerCreateHandler> createHandlers = new HashSet<>();

    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();
    private ContainerIdentity currentIdentity;
    private ZooKeeperLockManager lockManager;

    @Activate
    void activate(Map<String, ?> config) throws ProvisionException {
        currentIdentity = ContainerIdentity.createFrom(runtimeService.get().getIdentity());
        lockManager = new ZooKeeperLockManager(curator.get());
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
                        if (cntState.getProfileIdentities().contains(profileId)) {
                            LockHandle writeLock = cntState.aquireWriteLock();
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
            List<ContainerHandle> handles = Collections.emptyList();
            currentCnt = new ContainerState(null, currentIdentity, options, lockManager.readWriteLock(currentIdentity), handles);
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
        }
    }

    @Override
    public Provisioner getProvisioner(ContainerIdentity identity) {
        assertValid();
        IllegalArgumentAssertion.assertTrue(currentIdentity.equals(identity), "Provisioner for '" + identity + "' not supported");
        return provisioner.get();
    }

    @Override
    public LockHandle aquireContainerLock(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
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
        return createContainerInternal(getRequiredContainerState(parentId), options);
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
        ContainerIdentity identity = options.getIdentity();
        ContainerState cntState = new ContainerState(parentState, identity, options, lockManager.readWriteLock(identity), handles);
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
    public Container startContainer(ContainerIdentity identity, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        return startContainerInternal(cntState, listener);
    }

    private Container startContainerInternal(ContainerState cntState, ProvisionEventListener listener) throws ProvisionException {
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            LOGGER.info("Start container: {}", cntState);
            for (ContainerHandle handle : cntState.getContainerHandles()) {
                handle.start();
            }
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
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Container stopContainer(ContainerIdentity identity) {
        assertValid();
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot stop current container");
        ContainerState cntState = getRequiredContainerState(identity);
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
        IllegalStateAssertion.assertFalse(currentIdentity.equals(identity), "Cannot destroy current container");
        ContainerState cntState = getRequiredContainerState(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            IllegalStateAssertion.assertTrue(cntState.getChildIdentities().isEmpty(), "Cannot destroy a container that has active child containers: " + identity);

            // Stop the container
            if (cntState.getState() == State.STARTED) {
                stopContainer(identity);
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
        return getRequiredContainerState(currentIdentity).immutableContainer();
    }

    @Override
    public Container setProfileVersion(ContainerIdentity identity, Version version, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return setVersionInternal(cntState, version, listener);
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
    public Container addProfiles(ContainerIdentity identity, List<String> identities, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return addProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container addProfilesInternal(ContainerState cntState, List<String> identities, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Add container profiles: {} <= {}", cntState, identities);

        // Provision the profiles
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> effective = new ArrayList<>(cntState.getProfileIdentities());
        effective.addAll(identities);
        provisionProfilesInternal(cntState, version, effective, listener);

        // Update the references
        cntState.addProfiles(identities);
        return cntState.immutableContainer();
    }

    @Override
    public Container removeProfiles(ContainerIdentity identity, List<String> identities, ProvisionEventListener listener) throws ProvisionException {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        LockHandle writeLock = cntState.aquireWriteLock();
        try {
            return removeProfilesInternal(cntState, identities, listener);
        } finally {
            writeLock.unlock();
        }
    }

    private Container removeProfilesInternal(ContainerState cntState, List<String> identities, ProvisionEventListener listener) throws ProvisionException {
        LOGGER.info("Remove container profiles: {} => {}", cntState, identities);

        // Unprovision the profiles
        Version version = cntState.getProfileVersion().getIdentity();
        List<String> effective = new ArrayList<>(cntState.getProfileIdentities());
        effective.removeAll(identities);
        provisionProfilesInternal(cntState, version, effective, listener);

        // Update the references
        cntState.removeProfiles(identities);
        return cntState.immutableContainer();
    }

    @Override
    public Profile getEffectiveProfile(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        LockHandle readLock = cntState.aquireReadLock();
        try {
            Version version = cntState.getProfileVersion().getIdentity();
            List<String> identities = cntState.getProfileIdentities();
            return getEffectiveProfileInternal(cntState, version, identities);
        } finally {
            readLock.unlock();
        }
    }

    private Profile getEffectiveProfileInternal(ContainerState cntState, Version version, List<String> identities) {
        LockHandle readLock = cntState.aquireReadLock();
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

    private void provisionProfilesInternal(ContainerState cntState, Version version, List<String> identities, ProvisionEventListener listener)
            throws ProvisionException {
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
        ContainerState containerState = getRequiredContainerState(identity);
        return containerState.getServiceEndpoint(type);
    }

    @Override
    public ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity<?> endpointId) {
        ContainerState containerState = getRequiredContainerState(identity);
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
        LockHandle readLock = cntState.aquireReadLock();
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

    void bindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.bind(service);
    }

    void unbindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.unbind(service);
    }

    void bindCreateHandlers(ContainerCreateHandler service) {
        synchronized (createHandlers) {
            createHandlers.add(service);
        }
    }

    void unbindCreateHandlers(ContainerCreateHandler service) {
        synchronized (createHandlers) {
            createHandlers.remove(service);
        }
    }

    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }

    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
    }

    void bindCurator(CuratorFramework service) {
        curator.bind(service);
    }

    void unbindCurator(CuratorFramework service) {
        curator.unbind(service);
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
        private final AttributeSupport attributes = new AttributeSupport();
        private final List<String> profiles = new ArrayList<>();
        private final List<ContainerHandle> containerHandles = new ArrayList<>();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<>();
        private final Map<ResourceIdentity, ResourceHandle> resourceHandles = new LinkedHashMap<>();
        private final ReadWriteLock readWriteLock;
        private ProfileVersionState versionState;
        private State state;

        private ContainerState(ContainerState parentState, ContainerIdentity identity, CreateOptions options, ReadWriteLock readWriteLock, List<ContainerHandle> handles) {
            IllegalArgumentAssertion.assertNotNull(identity, "identity");
            IllegalArgumentAssertion.assertNotNull(options, "options");
            IllegalArgumentAssertion.assertNotNull(handles, "handles");
            this.parentState = parentState;
            this.identity = identity;
            this.state = State.CREATED;
            this.readWriteLock = readWriteLock;
            this.containerHandles.addAll(handles);
            if (parentState != null) {
                parentState.addChild(this);
            }
        }

        LockHandle aquireWriteLock() {
            final Lock writeLock = readWriteLock.writeLock();

            boolean success;
            try {
                success = writeLock.tryLock(10, TimeUnit.SECONDS);
            } catch (Exception ex) {
                success = false;
            }
            IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + identity);

            final LockHandle versionLock;
            if (versionState != null) {
                try {
                    versionLock = versionState.aquireWriteLock();
                } catch (Exception ex) {
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
            final Lock readLock = readWriteLock.readLock();

            boolean success;
            try {
                success = readLock.tryLock(10, TimeUnit.SECONDS);
            } catch (Exception ex) {
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

        ProfileVersion getProfileVersion() {
            return versionState != null ? versionState.getProfileVersion() : null;
        }

        List<String> getProfileIdentities() {
            LockHandle readLock = aquireReadLock();
            try {
                return Collections.unmodifiableList(new ArrayList<>(profiles));
            } finally {
                readLock.unlock();
            }
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
            for (ContainerHandle handle : containerHandles) {
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

        Map<ResourceIdentity, ResourceHandle> getResourceHandles() {
            return Collections.unmodifiableMap(resourceHandles);
        }

        void addResourceHandles(Map<ResourceIdentity, ResourceHandle> handles) {
            resourceHandles.putAll(handles);
        }

        void removeResourceHandles(Collection<ResourceIdentity> handles) {
            for (ResourceIdentity resid : handles) {
                resourceHandles.remove(resid);
            }
        }

        private List<ContainerHandle> getContainerHandles() {
            return Collections.unmodifiableList(containerHandles);
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

        private void addProfiles(List<String> identities) {
            assertNotDestroyed();
            LockHandle writeLock = aquireWriteLock();
            try {
                profiles.addAll(identities);
            } finally {
                writeLock.unlock();
            }
        }

        private void removeProfiles(List<String> identities) {
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
