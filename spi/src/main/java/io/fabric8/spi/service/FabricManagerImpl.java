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
package io.fabric8.spi.service;

import io.fabric8.api.Container;
import io.fabric8.api.FabricManager;
import io.fabric8.api.Host;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { FabricManager.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class FabricManagerImpl extends AbstractComponent implements FabricManager {

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
    public Set<Host> getHosts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Host getCurrentHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Container> getContainers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getCurrentContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Version> getVersions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Profile> getProfiles() {
        throw new UnsupportedOperationException();
    }

    @Reference
    void bindStateService(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindStateService(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }
}
