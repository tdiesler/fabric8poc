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
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.FabricException;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.api.VersionIdentity;
import io.fabric8.core.zookeeper.ZkPath;
import io.fabric8.spi.AbstractServiceEndpoint;
import io.fabric8.spi.AbstractURLServiceEndpoint;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.KeeperException;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry of stateful {@link Container} instances
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(immediate = true)
@Service(ContainerRegistry.class)
public final class ContainerRegistry extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRegistry.class);

    @Reference(referenceInterface = CuratorFramework.class)
    private ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    Container createContainer(ContainerIdentity parentId, ContainerIdentity identity, CreateOptions options, VersionIdentity version, List<ProfileIdentity> profiles, Set<ServiceEndpoint> endpoints) {
        IllegalStateAssertion.assertTrue(getContainer(identity) == null, "Container already exists: " + identity);
        Container cnt = new ImmutableContainer.Builder(identity, options.getRuntimeType(), options.getAttributes(), State.CREATED)
                .addParent(parentId)
                .addProfiles(profiles)
                .addProfileVersion(version)
                .addServiceEndpoints(endpoints)
                .build();

        storeInternal(cnt);
        return cnt;
    }

    Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        return Collections.unmodifiableSet(getContainerIdentitiesInternal());
    }

    void addChildToParent(ContainerIdentity parentId, ContainerIdentity childId) {
        ContainerLockManager.assertWriteLock(parentId);
        Container parent = getRequiredContainer(parentId);
        Set<ContainerIdentity> existingChildren = new LinkedHashSet<>(parent.getChildIdentities());
        existingChildren.add(childId);
        setParentInternal(childId, parentId);
        setChildIdentitiesInternal(parentId, existingChildren);
    }

    void removeChildFromParent(ContainerIdentity parentId, ContainerIdentity childId) {
        ContainerLockManager.assertWriteLock(parentId);
        Container parent = getRequiredContainer(parentId);
        Set<ContainerIdentity> existingChildren = new LinkedHashSet<>(parent.getChildIdentities());
        existingChildren.remove(childId);
        setParentInternal(childId, parentId);
        setChildIdentitiesInternal(parentId, existingChildren);
    }

    Container getContainer(ContainerIdentity identity) {
        ContainerLockManager.assertReadLock(identity);
        return readInternal(identity);
    }

    boolean hasContainer(ContainerIdentity identity) {
        return readInternal(identity) != null;
    }

    Container getRequiredContainer(ContainerIdentity identity) {
        Container container = readInternal(identity);
        IllegalStateAssertion.assertNotNull(container, "Container not registered: " + identity);
        return container;
    }

    Container startContainer(ContainerIdentity identity) {
        ContainerLockManager.assertWriteLock(identity);
        setStateInternal(identity, State.STARTED);
        return getRequiredContainer(identity);
    }

    Container stopContainer(ContainerIdentity identity) {
        ContainerLockManager.assertWriteLock(identity);
        setStateInternal(identity, State.STOPPED);
        return getRequiredContainer(identity);
    }

    Container destroyContainer(ContainerIdentity identity) {
        ContainerLockManager.assertWriteLock(identity);
        return removeInternal(identity);
    }

    Container setProfileVersion(ContainerIdentity identity, VersionIdentity version) {
        ContainerLockManager.assertWriteLock(identity);
        setVersionInternal(identity, version);
        return getRequiredContainer(identity);
    }

    Container addProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles) {
        ContainerLockManager.assertWriteLock(identity);
        List<ProfileIdentity> allProfiles = new ArrayList<>(getProfileIdentities(identity));
        allProfiles.addAll(profiles);
        setProfilesInternal(identity, allProfiles);
        return getRequiredContainer(identity);
    }

    Container removeProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles) {
        ContainerLockManager.assertWriteLock(identity);
        List<ProfileIdentity> allProfiles = new ArrayList<>(getProfileIdentities(identity));
        allProfiles.removeAll(profiles);
        setProfilesInternal(identity, allProfiles);
        return getRequiredContainer(identity);
    }

    ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity endpointId) {
        ContainerLockManager.assertReadLock(identity);
        return getServiceEndpointInternal(identity, endpointId);
    }

    Container addServiceEndpoint(ContainerIdentity identity, ServiceEndpoint endpoint) {
        ContainerLockManager.assertWriteLock(identity);
        assertValid();
        addServiceEndpointInternal(identity, endpoint);
        return getRequiredContainer(identity);
    }

    Container removeServiceEndpoint(ContainerIdentity identity, ServiceEndpoint endpoint) {
        ContainerLockManager.assertWriteLock(identity);
        assertValid();
        addServiceEndpointInternal(identity, endpoint);
        return getRequiredContainer(identity);
    }

    Container setServiceEndpoints(ContainerIdentity identity, Set<ServiceEndpoint> endpoint) {
        ContainerLockManager.assertWriteLock(identity);
        assertValid();
        setServiceEndpointsInternal(identity, endpoint);
        return getRequiredContainer(identity);
    }

    private void storeInternal(Container container) {
        LOGGER.debug("Storing container {}.", container.getIdentity());
        ContainerIdentity identity = container.getIdentity();
        String id = identity.getSymbolicName();
        Container existing = getContainer(identity);
        try {
            if (existing == null) {
                curator.get().create().creatingParentsIfNeeded().forPath(ZkPath.CONTAINER.getPath(id));
            }
            setParentInternal(identity, container.getParentIdentity());
            setChildIdentitiesInternal(identity, container.getChildIdentities());
            setRuntimeTypeInternal(identity, container.getRuntimeType());
            setProfilesInternal(identity, container.getProfileIdentities());
            setVersionInternal(identity, container.getProfileVersion());
            setAttributesInternal(ZkPath.CONTAINER_ATTRIBUTES.getPath(id), container.getAttributes());
            setStateInternal(identity, container.getState());
            setServiceEndpointsInternal(identity, container.getServiceEndpoints());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    private Container readInternal(ContainerIdentity identity) {
        LOGGER.debug("Reading container {}.", identity);
        Container result = null;
        String id = identity.getSymbolicName();
        try {
            if (curator.get().checkExists().forPath(ZkPath.CONTAINER.getPath(id)) != null) {

                ContainerIdentity parentIdentity = getParentInternal(identity);
                Map<AttributeKey<?>, Object> attributes = getAttributesInternal(ZkPath.CONTAINER_ATTRIBUTES.getPath(id));
                RuntimeType type = getRuntimeTypeInternal(identity);
                List<ProfileIdentity> profiles = getProfileIdentities(identity);
                VersionIdentity version = getVersionInternal(identity);
                State state = getStateInternal(identity);
                Set<ContainerIdentity> children = getChildIdentitiesInternal(identity);
                Set<ServiceEndpoint> endpoints = getServiceEndpointsInternal(identity);

                result = new ImmutableContainer.Builder(identity, type, attributes, state).addProfiles(profiles).addProfileVersion(version).addParent(parentIdentity)
                        .addChildren(children).addServiceEndpoints(endpoints).build();
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        }
        return result;
    }

    private Container removeInternal(ContainerIdentity identity) {
        LOGGER.debug("Storing container {}.", identity);
        setStateInternal(identity, State.DESTROYED);
        Container result = getContainer(identity);
        try {
            curator.get().delete().deletingChildrenIfNeeded().forPath(ZkPath.CONTAINER.getPath(identity.getSymbolicName()));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
        return result;
    }

    private Set<ContainerIdentity> getContainerIdentitiesInternal() {
        Set<ContainerIdentity> identities = new LinkedHashSet<>();
        try {
            String containersPath = ZkPath.CONTAINERS.getPath();
            List<String> containers = curator.get().getChildren().forPath(containersPath);
            for (String container : containers) {
                identities.add(ContainerIdentity.create(container));
            }
            return Collections.unmodifiableSet(identities);
        } catch (KeeperException.NoNodeException ex) {
            return Collections.emptySet();
        } catch (Exception e) {
            throw new FabricException("Failed to list containers.", e);
        }
    }

    /**
     * Reads the {@link io.fabric8.api.Container.State} of the {@link Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          The {@link io.fabric8.api.Container.State}
     */
    private Container.State getStateInternal(ContainerIdentity identity) {
        String id = identity.getSymbolicName();
        try {
            String data = new String(curator.get().getData().forPath(ZkPath.CONTAINER_STATE.getPath(id)));
            if (data != null && !data.isEmpty()) {
                return State.valueOf(data);
            } else {
                return State.CREATED;
            }
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " state.", e);
        }
    }

    /**
     * Sets the {@link io.fabric8.api.Container.State} of the {@link io.fabric8.api.Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity          The target {@link io.fabric8.api.ContainerIdentity}.
     * @param state             The state {@link io.fabric8.api.ContainerIdentity}.
     */
    private void setStateInternal(ContainerIdentity identity, State state) {
        String id = identity.getSymbolicName();
        try {
            String containersParentPath = ZkPath.CONTAINER_STATE.getPath(id);
            if (curator.get().checkExists().forPath(containersParentPath) != null) {
                curator.get().setData().forPath(containersParentPath, state.name().getBytes());
            } else {
                curator.get().create().forPath(containersParentPath, state.name().getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " state.", e);
        }
    }

    /**
     * Reads the {@link io.fabric8.api.ContainerIdentity} of the parent of the {@link Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          The parent's {@link io.fabric8.api.ContainerIdentity}.
     */
    private ContainerIdentity getParentInternal(ContainerIdentity identity) {
        String id = identity.getSymbolicName();
        try {
            String data = new String(curator.get().getData().forPath(ZkPath.CONTAINER_PARENT.getPath(id))).trim();
            if (data != null && !data.isEmpty()) {
                return ContainerIdentity.createFrom(data);
            } else {
                return null;
            }
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " parent identity.", e);
        }
    }

    /**
     * Sets the parent identity of the {@link io.fabric8.api.Container}.
     * @param identity          The target {@link io.fabric8.api.ContainerIdentity}.
     * @param parentIdentity    The parent {@link io.fabric8.api.ContainerIdentity}.
     */
    private void setParentInternal(ContainerIdentity identity, ContainerIdentity parentIdentity) {
        String id = identity.getSymbolicName();
        byte[] parentId = parentIdentity != null ? parentIdentity.getSymbolicName().getBytes() : new byte[0];

        try {
            String containersParentPath = ZkPath.CONTAINER_PARENT.getPath(id);
            if (curator.get().checkExists().forPath(containersParentPath) != null) {
                curator.get().setData().forPath(containersParentPath, parentId);
            } else {
                curator.get().create().creatingParentsIfNeeded().forPath(containersParentPath, parentId);
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " parent identity.", e);
        }
    }

    /**
     * Reads the {@link io.fabric8.api.ContainerIdentity} of the children of the {@link Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          A set containing the children {@link io.fabric8.api.ContainerIdentity} items.
     */
    private Set<ContainerIdentity> getChildIdentitiesInternal(ContainerIdentity identity) {
        Set<ContainerIdentity> childIdentities = new LinkedHashSet<>();
        String id = identity.getSymbolicName();
        try {
            String data = new String(curator.get().getData().forPath(ZkPath.CONTAINER_CHILDREN.getPath(id))).trim();
            if (!data.isEmpty()) {
                for (String child : data.split(" +")) {
                    childIdentities.add(ContainerIdentity.create(child));
                }
            }
            return Collections.unmodifiableSet(childIdentities);
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " child identities.", e);
        }
    }

    /**
     * Sets the child {@link io.fabric8.api.ContainerIdentity} of the {@link io.fabric8.api.Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity          The target {@link io.fabric8.api.ContainerIdentity}.
     * @param childIdentities   A set with the {@link io.fabric8.api.ContainerIdentity} of the children.
     */
    private void setChildIdentitiesInternal(ContainerIdentity identity, Set<ContainerIdentity> childIdentities) {
        String id = identity.getSymbolicName();
        String data = joinContainerIdentities(childIdentities, " ").trim();

        try {
            String containerChildren = ZkPath.CONTAINER_CHILDREN.getPath(id);
            if (curator.get().checkExists().forPath(containerChildren) != null) {
                curator.get().setData().forPath(containerChildren, data.getBytes());
            } else {
                curator.get().create().creatingParentsIfNeeded().forPath(containerChildren, data.getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " children.", e);
        }
    }

    /**
     * Reads the {@link org.jboss.gravia.runtime.RuntimeType} of the {@link Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          The {@link org.jboss.gravia.runtime.RuntimeType}.
     */
    private RuntimeType getRuntimeTypeInternal(ContainerIdentity identity) {
        String id = identity.getSymbolicName();
        try {
            String data = new String(curator.get().getData().forPath(ZkPath.CONTAINER_TYPE.getPath(id)));
            return RuntimeType.valueOf(data);
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " runtime type.", e);
        }
    }

    /**
     * Reads the {@link org.jboss.gravia.runtime.RuntimeType} of the {@link Container} with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity      The identity of the {@link Container}.
     * @param runtimeType   The {@link org.jboss.gravia.runtime.RuntimeType}.
     */
    private void setRuntimeTypeInternal(ContainerIdentity identity, RuntimeType runtimeType) {
        String id = identity.getSymbolicName();
        String data = runtimeType.name();

        try {
            String containerTypePath = ZkPath.CONTAINER_TYPE.getPath(id);
            if (curator.get().checkExists().forPath(containerTypePath) != null) {
                curator.get().setData().forPath(containerTypePath, data.getBytes());
            } else {
                curator.get().create().forPath(containerTypePath, data.getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " runtime type.", e);
        }
    }

    /**
     * Reads the identities of the {@link io.fabric8.api.Profile} items associated with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          A List of Profile Identities.
     */
    private List<ProfileIdentity> getProfileIdentities(ContainerIdentity identity) {
        final List<ProfileIdentity> profiles = new ArrayList<>();
        try {
            for (String prfid : new String(curator.get().getData().forPath(ZkPath.CONTAINER_CONFIG_PROFILES.getPath(identity.getSymbolicName()))).split(" +")) {
                profiles.add(ProfileIdentity.createFrom(prfid));
            }
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " profile identities.", e);
        }
        return Collections.unmodifiableList(profiles);
    }

    /**
     * Sets the {@link io.fabric8.api.Profile} identities of the {@link io.fabric8.api.Container}.
     * @param identity          The target {@link io.fabric8.api.ContainerIdentity}.
     * @param profiles    The parent {@link io.fabric8.api.ContainerIdentity}.
     */
    private void setProfilesInternal(ContainerIdentity identity, List<ProfileIdentity> profiles) {
        String id = identity.getSymbolicName();
        StringBuffer dataBuffer = new StringBuffer();
        for (ProfileIdentity prfid : profiles) {
            dataBuffer.append(prfid.getCanonicalForm() + " ");
        }
        String data = dataBuffer.toString().trim();

        try {
            String containerProfilesPath = ZkPath.CONTAINER_CONFIG_PROFILES.getPath(id);
            if (curator.get().checkExists().forPath(containerProfilesPath) != null) {
                curator.get().setData().forPath(containerProfilesPath, data.getBytes());
            } else {
                curator.get().create().creatingParentsIfNeeded().forPath(containerProfilesPath, data.getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " profiles.", e);
        }
    }

    /**
     * Reads the {@link Version} associated with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          The {@link Version}.
     */
    private VersionIdentity getVersionInternal(ContainerIdentity identity) {
        try {
            String data = new String(curator.get().getData().forPath(ZkPath.CONTAINER_CONFIG_VERSION.getPath(identity.getSymbolicName())));
            return VersionIdentity.createFrom(data);
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " version.", e);
        }
    }

    /**
     * Writes the {@link Version} associated with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     */
    private void setVersionInternal(ContainerIdentity identity, VersionIdentity version) {
        String id = identity.getCanonicalForm();
        String data = version.getCanonicalForm();
        try {
            String containerVersionPath = ZkPath.CONTAINER_CONFIG_VERSION.getPath(id);
            if (curator.get().checkExists().forPath(containerVersionPath) != null) {
                curator.get().setData().forPath(containerVersionPath, data.getBytes());
            } else {
                curator.get().create().creatingParentsIfNeeded().forPath(containerVersionPath, data.getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " version.", e);
        }
    }

    /**
     * Reads the attributes stored under the specified path.
     * @param basePath      The path that contains the attributes
     * @return          A {@link Map} of {@link io.fabric8.api.AttributeKey} -> Value.
     */
    private Map<AttributeKey<?>, Object> getAttributesInternal(String basePath) {
        Map<AttributeKey<?>, Object> attributes = new LinkedHashMap<>();
        try {
            for (String path : curator.get().getChildren().forPath(basePath)) {
                String attributeKeyPath = ZKPaths.makePath(basePath, path);
                String attributeValuePath = ZKPaths.makePath(attributeKeyPath, "value");
                String keyData = new String(curator.get().getData().forPath(attributeKeyPath));
                String valueData = new String(curator.get().getData().forPath(attributeValuePath));
                AttributeKey<?> key = AttributeKey.createFrom(keyData);
                Object value = key.getFactory().createFrom(valueData);
                attributes.put(key, value);
            }
            return attributes;
        } catch (Exception e) {
            throw new FabricException("Failed to read attributes from:" + basePath + ".", e);
        }
    }

    /**
    * Reads the attributes stored under the specified path.
    * @param basePath      The path that contains the attributes
    * @param attributes    A {@link Map} of {@link io.fabric8.api.AttributeKey} -> Value.
    */
    private void setAttributesInternal(String basePath, Map<AttributeKey<?>, Object> attributes) {
        try {
            if (curator.get().checkExists().forPath(basePath) != null) {
                curator.get().delete().deletingChildrenIfNeeded().forPath(basePath);
            }
            curator.get().create().creatingParentsIfNeeded().forPath(basePath);

            for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
                AttributeKey<?> key = entry.getKey();
                Object value = entry.getValue();
                String attributeKeyPath = ZKPaths.makePath(basePath, key.getName());
                String attributeValuePath = ZKPaths.makePath(attributeKeyPath, "value");
                String data = value.toString();
                curator.get().create().creatingParentsIfNeeded().forPath(attributeKeyPath, key.getCanonicalForm().getBytes());
                curator.get().create().creatingParentsIfNeeded().forPath(attributeValuePath, data.getBytes());
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write attributes at:" + basePath + ".", e);
        }
    }

    /**
     * Writes the {@link io.fabric8.api.ServiceEndpoint} items associated with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @param endpoints The set of {@link io.fabric8.api.ServiceEndpoint} items.
     */
    private void setServiceEndpointsInternal(ContainerIdentity identity, Set<ServiceEndpoint> endpoints) {
        String containerEndpointsPath = ZkPath.CONTAINER_ENDPOINTS.getPath(identity.getSymbolicName());
        try {
            if (curator.get().checkExists().forPath(containerEndpointsPath) != null) {
                curator.get().delete().deletingChildrenIfNeeded().forPath(containerEndpointsPath);
            }
            curator.get().create().creatingParentsIfNeeded().forPath(containerEndpointsPath);

            for (ServiceEndpoint endpoint : endpoints) {
                ServiceEndpointIdentity endpointId = endpoint.getIdentity();
                String endpointPath = ZKPaths.makePath(containerEndpointsPath, endpointId.getSymbolicName());
                curator.get().create().creatingParentsIfNeeded().forPath(endpointPath);
                setAttributesInternal(ZKPaths.makePath(endpointPath, "attributes"), endpoint.getAttributes());

            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " endpoints.", e);
        }
    }

    /**
     * Reads the {@link io.fabric8.api.ServiceEndpoint} items associated with the specified {@link io.fabric8.api.ContainerIdentity}.
     * @param identity  The identity of the {@link Container}.
     * @return          The set of {@link ServiceEndpoint} items.
     */
    private Set<ServiceEndpoint> getServiceEndpointsInternal(ContainerIdentity identity) {
        Set<ServiceEndpoint> endpoints = new LinkedHashSet<>();
        String containerEndpointsPath = ZkPath.CONTAINER_ENDPOINTS.getPath(identity.getSymbolicName());
        try {
            List<String> names = curator.get().getChildren().forPath(containerEndpointsPath);

            for (String name : names) {
                endpoints.add(getServiceEndpointInternal(identity, ServiceEndpointIdentity.create(name)));
            }
        } catch (KeeperException.NoNodeException e) {
            return Collections.emptySet();
        } catch (Exception e) {
            throw new FabricException("Failed to read container's:" + identity.getSymbolicName() + " endpoints.", e);
        }
        return endpoints;
    }

    private ServiceEndpoint getServiceEndpointInternal(ContainerIdentity identity, ServiceEndpointIdentity endpointId) {
        String id = identity.getSymbolicName();
        String endpointPath = ZkPath.CONTAINER_ENDPOINT.getPath(id, endpointId.getSymbolicName());
        try {
            Map<AttributeKey<?>, Object> attributes = getAttributesInternal(ZKPaths.makePath(endpointPath, "attributes"));
            if (attributes.containsKey(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL)) {
                return new AbstractURLServiceEndpoint(endpointId, attributes);
            } else {
                return new AbstractServiceEndpoint(endpointId, attributes);
            }
        } catch (Exception e) {
            throw new FabricException("Failed to read endpoint from:" + endpointPath);
        }
    }

    /**
     * Associates a {@link io.fabric8.api.ContainerIdentity} with a {@link io.fabric8.api.ServiceEndpoint}.
     * @param identity  The identity of the container.
     * @param endpoint  The service endpoint.
     */
    private void addServiceEndpointInternal(ContainerIdentity identity, ServiceEndpoint endpoint) {
        ServiceEndpointIdentity endpointId = endpoint.getIdentity();
        String endpointPath = ZkPath.CONTAINER_ENDPOINT.getPath(identity.getSymbolicName(), endpointId.getSymbolicName());
        try {
            if (curator.get().checkExists().forPath(endpointPath) != null) {
                curator.get().delete().deletingChildrenIfNeeded().forPath(endpointPath);
            }
            curator.get().create().creatingParentsIfNeeded().forPath(endpointPath);

            curator.get().create().creatingParentsIfNeeded().forPath(endpointPath);
            setAttributesInternal(ZKPaths.makePath(endpointPath, "attributes"), endpoint.getAttributes());
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " endpoints.", e);
        }
    }

    /**
     * Removes the association of a {@link io.fabric8.api.ContainerIdentity} with a {@link io.fabric8.api.ServiceEndpoint}.
     * [TODO] #51 Add support for removeServiceEndpoint
     * @param identity  The identity of the container.
     * @param endpoint  The service endpoint.
     */
    @SuppressWarnings("unused")
    private void removeServiceEndpointInternal(ContainerIdentity identity, ServiceEndpoint endpoint) {
        ServiceEndpointIdentity endpointId = endpoint.getIdentity();
        String endpointPath = ZkPath.CONTAINER_ENDPOINT.getPath(identity.getSymbolicName(), endpointId.getSymbolicName());
        try {
            if (curator.get().checkExists().forPath(endpointPath) != null) {
                curator.get().delete().deletingChildrenIfNeeded().forPath(endpointPath);
            }
        } catch (Exception e) {
            throw new FabricException("Failed to write container's:" + identity.getSymbolicName() + " endpoints.", e);
        }
    }

    private static String joinContainerIdentities(Iterable<ContainerIdentity> identities, String separator) {
        StringBuilder builder = new StringBuilder();
        for (ContainerIdentity identity : identities) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(identity.getSymbolicName());
        }
        return builder.toString();
    }

    void bindCurator(CuratorFramework service) {
        curator.bind(service);
    }

    void unbindCurator(CuratorFramework service) {
        curator.unbind(service);
    }
}
