/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */
package io.fabric8.core.internal;

import java.util.concurrent.atomic.AtomicLong;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.spi.ClusterDataStore;
import io.fabric8.spi.scr.AbstractComponent;

import org.jboss.gravia.utils.NotNullException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

/**
 * A cluster wide data store
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
@Component(service = { ClusterDataStore.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ClusterDataStoreImpl extends AbstractComponent implements ClusterDataStore {

    // [TODO] Real cluster wide identities
    private final AtomicLong uniqueTokenGenerator = new AtomicLong();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public ContainerIdentity createContainerIdentity(ContainerIdentity parentId, String prefix) {
        NotNullException.assertValue(prefix, "prefix");
        String parentName = parentId != null ? parentId.getSymbolicName() + ":" : "";
        ContainerIdentity containerId = ContainerIdentity.create(parentName + prefix + "#" + uniqueTokenGenerator.incrementAndGet());
        return containerId;
    }
}
