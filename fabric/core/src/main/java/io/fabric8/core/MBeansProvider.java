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

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.api.management.ProfileVersionManagement;
import io.fabric8.spi.management.ContainerOpenType;
import io.fabric8.spi.management.ProfileOpenType;
import io.fabric8.spi.management.ProfileVersionOpenType;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;

/**
 * A provider of system MBeans
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(MBeansProvider.class)
public final class MBeansProvider extends AbstractComponent {

    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();
    @Reference(referenceInterface = ContainerManager.class)
    private final ValidatingReference<ContainerManager> containerManager = new ValidatingReference<ContainerManager>();
    @Reference(referenceInterface = ProfileManager.class)
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
            ContainerManagement cntManagement = new ContainerManagementMBean(containerManager.get());
            server.registerMBean(new StandardMBean(cntManagement, ContainerManagement.class, false), ContainerManagement.OBJECT_NAME);

            ProfileVersionManagement prvManagement = new ProfileVersionManagementMBean(profileManager.get());
            server.registerMBean(new StandardMBean(prvManagement, ProfileVersionManagement.class, false), ProfileVersionManagement.OBJECT_NAME);

            ProfileManagement prfManagement = new ProfileManagementMBean(profileManager.get());
            server.registerMBean(new StandardMBean(prfManagement, ProfileManagement.class, false), ProfileManagement.OBJECT_NAME);
        } catch (JMException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void deactivateInternal() {
        MBeanServer server = mbeanServer.get();
        try {
            server.unregisterMBean(ContainerManagement.OBJECT_NAME);
            server.unregisterMBean(ProfileVersionManagementMBean.OBJECT_NAME);
            server.unregisterMBean(ProfileManagement.OBJECT_NAME);
        } catch (JMException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void bindContainerManager(ContainerManager service) {
        this.containerManager.bind(service);
    }

    void unbindContainerManager(ContainerManager service) {
        this.containerManager.unbind(service);
    }

    void bindMbeanServer(MBeanServer service) {
        this.mbeanServer.bind(service);
    }

    void unbindMbeanServer(MBeanServer service) {
        this.mbeanServer.unbind(service);
    }

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
            for (ContainerIdentity cntid : containerManager.getContainerIdentities()) {
                result.add(cntid.toString());
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public CompositeData getContainer(String identity) {
            Container container = containerManager.getContainer(ContainerIdentity.create(identity));
            return container != null ? ContainerOpenType.getCompositeData(container) : null;
        }

    }

    static class ProfileVersionManagementMBean implements ProfileVersionManagement {

        private final ProfileManager profileManager;

        ProfileVersionManagementMBean(ProfileManager profileManager) {
            this.profileManager = profileManager;
        }

        @Override
        public Set<String> getProfileVersionIds() {
            Set<String> result = new HashSet<String>();
            for (Version version : profileManager.getVersions()) {
                result.add(version.toString());
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public CompositeData getProfileVersion(String identity) {
            ProfileVersion pversion = profileManager.getProfileVersion(Version.parseVersion(identity));
            return pversion != null ? ProfileVersionOpenType.getCompositeData(pversion) : null;
        }
    }

    static class ProfileManagementMBean implements ProfileManagement {

        private final ProfileManager profileManager;

        ProfileManagementMBean(ProfileManager profileManager) {
            this.profileManager = profileManager;
        }

        @Override
        public Set<String> getProfileIds(String version) {
            Set<String> result = new HashSet<String>();
            for (String prfid : profileManager.getProfileIdentities(Version.parseVersion(version))) {
                result.add(prfid.toString());
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public CompositeData getProfile(String version, String identity) {
            Profile profile = profileManager.getProfile(Version.parseVersion(version), identity);
            return profile != null ? ProfileOpenType.getCompositeData(profile) : null;
        }
    }
}
