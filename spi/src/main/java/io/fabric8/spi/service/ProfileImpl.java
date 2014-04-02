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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.ProfileState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.utils.NotNullException;

final class ProfileImpl implements Profile {

    private final Set<ContainerIdentity> containers = new HashSet<ContainerIdentity>();
    private final List<Resource> resources = new ArrayList<Resource>();
    private final ProfileIdentity identity;

    ProfileImpl(ProfileState profileState) {
        NotNullException.assertValue(profileState, "profileState");
        this.identity = profileState.getIdentity();
        for (ContainerState cntState : profileState.getContainers()) {
            this.containers.add(cntState.getIdentity());
        }
        this.resources.addAll(profileState.getResources());
    }

    ProfileImpl(ProfileIdentity identity) {
        NotNullException.assertValue(identity, "identity");
        this.identity = identity;
    }

    @Override
    public ProfileIdentity getIdentity() {
        return identity;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ContainerIdentity> getContainers() {
        return Collections.unmodifiableSet(containers);
    }

    @Override
    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProfileImpl))
            return false;
        ProfileImpl other = (ProfileImpl) obj;
        return other.identity.equals(identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        return identity.toString();
    }
}
