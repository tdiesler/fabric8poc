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

import io.fabric8.core.api.Container;
import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.ContainerManager;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.api.management.ContainerManagement;
import io.fabric8.core.api.management.ProfileManagement;
import io.fabric8.core.spi.scr.AbstractComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.StandardMBean;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { MBeansProvider.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class MBeansProvider extends AbstractComponent {

    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();
    private final ValidatingReference<ContainerManager> containerManager = new ValidatingReference<ContainerManager>();
    private final ValidatingReference<ProfileManager> profileManager = new ValidatingReference<ProfileManager>();

    @Activate
    void activate() {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    private void activateInternal() {
        MBeanServer server = mbeanServer.get();
        try {
            server.registerMBean(new StandardMBean(new ContainerManagementMBean(), ContainerManagement.class, false), ContainerManagement.OBJECT_NAME);
            server.registerMBean(new StandardMBean(new ProfileManagementMBean(), ProfileManagement.class, false), ProfileManagement.OBJECT_NAME);
        } catch (JMException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void deactivateInternal() {
        MBeanServer server = mbeanServer.get();
        try {
            server.unregisterMBean(ContainerManagement.OBJECT_NAME);
            server.unregisterMBean(ProfileManagement.OBJECT_NAME);
        } catch (JMException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Reference
    void bindContainerManager(ContainerManager service) {
        this.containerManager.bind(service);
    }

    void unbindContainerManager(ContainerManager service) {
        this.containerManager.unbind(service);
    }

    @Reference
    void bindMBeanServer(MBeanServer service) {
        this.mbeanServer.bind(service);
    }

    void unbindMBeanServer(MBeanServer service) {
        this.mbeanServer.unbind(service);
    }

    @Reference
    void bindProfileManager(ProfileManager service) {
        this.profileManager.bind(service);
    }

    @Reference
    void unbindProfileManager(ProfileManager service) {
        this.profileManager.unbind(service);
    }

    class ContainerManagementMBean implements ContainerManagement {

        @Override
        public Set<ContainerIdentity> getContainerIds() {
            return containerManager.get().getContainerIds();
        }

        @Override
        public Container getContainer(ContainerIdentity identity) {
            return containerManager.get().getContainer(identity);
        }
    }

    class ProfileManagementMBean implements ProfileManagement {

        @Override
        public Set<Version> getProfileVersionIds() {
            return profileManager.get().getProfileVersionIds();
        }

        @Override
        public ProfileVersion getProfileVersion(Version identity) {
            return profileManager.get().getProfileVersion(identity);
        }

        @Override
        public Set<ProfileIdentity> getProfileIds(Version version) {
            return profileManager.get().getProfileIds(version);
        }

        @Override
        public Profile getProfile(Version version, ProfileIdentity identity) {
            return profileManager.get().getProfile(version, identity);
        }
    }
}