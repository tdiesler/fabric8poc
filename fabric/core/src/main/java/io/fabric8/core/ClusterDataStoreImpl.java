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

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.FabricException;
import io.fabric8.spi.ClusterDataStore;
import io.fabric8.spi.scr.AbstractComponent;

import io.fabric8.spi.scr.ValidatingReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * A cluster wide data store
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ClusterDataStore.class)
public final class ClusterDataStoreImpl extends AbstractComponent implements ClusterDataStore {

    private static final String COUNTER_PATH = "/fabric/registry/uuid/containers/%s";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public ContainerIdentity createContainerIdentity(ContainerIdentity parentId, String name) {
        assertValid();
        IllegalArgumentAssertion.assertNotNull(name, "prefix");
        int increment = getIncrementForPrefix(name);
        String parentPrefix = parentId != null ? parentId.getSymbolicName() + ":" : "";
        String suffix =  increment > 1 ? String.valueOf(increment) : "";
        ContainerIdentity containerId = ContainerIdentity.create(parentPrefix + name + "#" + suffix);
        return containerId;
    }

    private int getIncrementForPrefix(String prefix) {
        String path = String.format(COUNTER_PATH, prefix);
        SharedCount sharedCount = new SharedCount(curator.get(), path, 1);
        try {
            sharedCount.start();
            for (int count = sharedCount.getCount(); true; count = sharedCount.getCount()) {
                if (sharedCount.trySetCount(count + 1)) {
                    return count;
                }
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        } finally {
            IOUtils.safeClose(sharedCount);
        }
    }

    void bindCurator(CuratorFramework service) {
        curator.bind(service);
    }
    void unbindCurator(CuratorFramework service) {
        curator.unbind(service);
    }
}
