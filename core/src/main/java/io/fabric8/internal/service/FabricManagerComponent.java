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
package io.fabric8.internal.service;

import io.fabric8.api.Container;
import io.fabric8.api.FabricManager;
import io.fabric8.internal.api.PermitManager;
import io.fabric8.internal.api.PermitManager.Permit;
import io.fabric8.internal.scr.AbstractComponent;
import io.fabric8.internal.scr.ValidatingReference;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { FabricManager.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class FabricManagerComponent extends AbstractComponent implements FabricManager {

    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public Container createContainer(String name) {
        Permit<FabricService> permit = permitManager.get().aquirePermit(FabricService.PROTECTED_STATE, false);
        try {
            FabricService fabricService = permit.getInstance();
            return new ContainerImpl(fabricService.createContainer(name));
        } finally {
            permit.release();
        }
    }

    @Override
    public Container getContainer(String name) {
        Permit<FabricService> permit = permitManager.get().aquirePermit(FabricService.PROTECTED_STATE, false);
        try {
            FabricService fabricService = permit.getInstance();
            return new ContainerImpl(fabricService.getContainerByName(name));
        } finally {
            permit.release();
        }
    }

    @Reference
    void bindStateService(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindStateService(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    class ContainerImpl implements Container {

        private final ContainerState delegate;

        ContainerImpl(ContainerState delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public State getState() {
            return delegate.getState();
        }

        @Override
        public void start() {
            Permit<FabricService> permit = permitManager.get().aquirePermit(FabricService.PROTECTED_STATE, false);
            try {
                FabricService fabricService = permit.getInstance();
                fabricService.startContainer(getName());
            } finally {
                permit.release();
            }
        }

        @Override
        public void stop() {
            Permit<FabricService> permit = permitManager.get().aquirePermit(FabricService.PROTECTED_STATE, false);
            try {
                FabricService fabricService = permit.getInstance();
                fabricService.stopContainer(getName());
            } finally {
                permit.release();
            }
        }

        @Override
        public void destroy() {
            Permit<FabricService> permit = permitManager.get().aquirePermit(FabricService.PROTECTED_STATE, false);
            try {
                FabricService fabricService = permit.getInstance();
                fabricService.destroyContainer(getName());
            } finally {
                permit.release();
            }
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
