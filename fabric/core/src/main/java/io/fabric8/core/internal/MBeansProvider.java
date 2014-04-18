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

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashSet;
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

/**
 * A provider of system MBeans
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Mar-2014
 */
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
            ContainerManagementMBean containerManagement = new ContainerManagementMBean(containerManager.get());
            server.registerMBean(new StandardMBean(containerManagement, ContainerManagement.class, false), ContainerManagement.OBJECT_NAME);
            ProfileManagementMBean profileManagement = new ProfileManagementMBean(profileManager.get());
            server.registerMBean(new StandardMBean(profileManagement, ProfileManagement.class, false), ProfileManagement.OBJECT_NAME);
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

    void unbindProfileManager(ProfileManager service) {
        this.profileManager.unbind(service);
    }

    static class ContainerManagementMBean implements ContainerManagement {

        private final ContainerManager containerManager;

        ContainerManagementMBean(ContainerManager containerManager) {
            this.containerManager = containerManager;
        }

        @Override
        public Set<String> getContainerIds() {
            Set<String> result = new HashSet<String>();
            for (ContainerIdentity cntid : containerManager.getContainerIds()) {
                result.add(cntid.toString());
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public String getContainerState(String identity) {
            ContainerIdentity cntid = ContainerIdentity.create(identity);
            Container cnt = containerManager.getContainer(cntid);
            return cnt != null ? cnt.getState().toString() : null;
        }
    }

    static class ProfileManagementMBean implements ProfileManagement {

        private final ProfileManager profileManager;

        ProfileManagementMBean(ProfileManager profileManager) {
            this.profileManager = profileManager;
        }

        @Override
        public Set<String> getProfileVersionIds() {
            Set<String> result = new HashSet<String>();
            for (Version version : profileManager.getProfileVersionIds()) {
                result.add(version.toString());
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Set<String> getProfileIds(String version) {
            Set<String> result = new HashSet<String>();
            for (ProfileIdentity prfid : profileManager.getProfileIds(Version.parseVersion(version))) {
                result.add(prfid.toString());
            }
            return Collections.unmodifiableSet(result);
        }
    }
}