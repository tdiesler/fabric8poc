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
    @Reference
    private RuntimeService runtimeService;

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }
}
