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
package io.fabric8.spi;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.spi.permit.PermitState;

import java.util.concurrent.locks.Lock;

import org.jboss.gravia.resource.Version;

/**
 * The internal profile service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileService extends ProfileManager {

    /**
     * The {@link PermitState} that protects this service.
     */
    PermitState<ProfileService> PERMIT = new PermitState<ProfileService>(ProfileService.class);

    /**
     * Aquire a lock on the give profile version
     */
    Lock aquireProfileVersionLock(Version version);

    void addContainerToProfileVersion(Version version, ContainerIdentity containerId);

    void removeContainerFromProfileVersion(Version version, ContainerIdentity containerId);

    void addContainerToProfile(Version version, ProfileIdentity profileId, ContainerIdentity containerId);

    void removeContainerFromProfile(Version version, ProfileIdentity profileId, ContainerIdentity containerId);
}
