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

import io.fabric8.api.ContainerManager;
import io.fabric8.api.ProfileManager;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.Provisioner;

/**
 * Implementation of the the {@link BootstrapComplete} marker service
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(BootstrapComplete.class)
public final class BootstrapCompleteImpl extends AbstractComponent implements BootstrapComplete {

    @Reference
    private RuntimeService runtimeService;
    @Reference
    private BootConfiguration bootConfiguration;
    @Reference
    private BootstrapService bootstrapService;
    @Reference
    private ContainerManager containerManager;
    @Reference
    private ContainerService containerService;
    @Reference
    private MBeansProvider mBeansProvider;
    @Reference
    private ProfileManager profileManager;
    @Reference
    private Provisioner provisioner;

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void activateInternal() {
    }

    protected void bindRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    protected void unbindRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = null;
    }

    protected void bindBootConfiguration(BootConfiguration bootConfiguration) {
        this.bootConfiguration = bootConfiguration;
    }

    protected void unbindBootConfiguration(BootConfiguration bootConfiguration) {
        this.bootConfiguration = null;
    }

    protected void bindBootstrapService(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    protected void unbindBootstrapService(BootstrapService bootstrapService) {
        this.bootstrapService = null;
    }

    protected void bindContainerManager(ContainerManager containerManager) {
        this.containerManager = containerManager;
    }

    protected void unbindContainerManager(ContainerManager containerManager) {
        this.containerManager = null;
    }

    protected void bindContainerService(ContainerService containerService) {
        this.containerService = containerService;
    }

    protected void unbindContainerService(ContainerService containerService) {
        this.containerService = null;
    }

    protected void bindMBeansProvider(MBeansProvider mBeansProvider) {
        this.mBeansProvider = mBeansProvider;
    }

    protected void unbindMBeansProvider(MBeansProvider mBeansProvider) {
        this.mBeansProvider = null;
    }

    protected void bindProfileManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    protected void unbindProfileManager(ProfileManager profileManager) {
        this.profileManager = null;
    }

    protected void bindProvisioner(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    protected void unbindProvisioner(Provisioner provisioner) {
        this.provisioner = null;
    }

}
