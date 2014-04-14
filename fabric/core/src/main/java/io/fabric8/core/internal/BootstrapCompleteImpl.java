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
package io.fabric8.core.internal;

import io.fabric8.api.ContainerBuilderFactory;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ProfileManager;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.scr.AbstractComponent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { BootstrapComplete.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class BootstrapCompleteImpl extends AbstractComponent implements BootstrapComplete {

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Reference
    void bindContainerManager(ContainerManager service) {
    }

    void unbindContainerManager(ContainerManager service) {
    }

    @Reference
    void bindProfileManager(ProfileManager service) {
    }

    void unbindProfileManager(ProfileManager service) {
    }

    @Reference
    void bindContainerService(ContainerService service) {
    }

    void unbindContainerService(ContainerService service) {
    }

    @Reference
    void bindContainerBuilderFactory(ContainerBuilderFactory<?> service) {
    }

    void unbindContainerBuilderFactory(ContainerBuilderFactory<?> service) {
    }

    @Reference
    void bindMBeansProvider(MBeansProvider service) {
    }

    void unbindMBeansProvider(MBeansProvider service) {
    }
}
