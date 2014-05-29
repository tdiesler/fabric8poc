/*
 * #%L
 * Fabric8 :: SPI
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
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileVersion;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

public abstract class AbstractCreateOptions implements CreateOptions {

    private final AttributeSupport attributes = new AttributeSupport();
    private ContainerIdentity identity;
    private Version version = ProfileVersion.DEFAULT_PROFILE_VERSION_IDENTITY;
    private Set<String> profiles = new LinkedHashSet<>(Arrays.asList(Profile.DEFAULT_PROFILE_IDENTITY));

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Set<String> getProfiles() {
        return profiles;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributes.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    protected void validate() {
        IllegalStateAssertion.assertNotNull(identity, "Identity cannot be null");
    }

    // Setters are protected

    protected void setIdentity(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        this.identity = identity;
    }
    
    protected void setVersion(Version version) {
        IllegalArgumentAssertion.assertNotNull(version, "version");
        this.version = version;
    }

    protected void setProfiles(Set<String> profiles) {
        IllegalArgumentAssertion.assertNotNull(profiles, "profiles");
        this.profiles = profiles;
    }

    protected <T> void putAttribute(AttributeKey<T> key, T value) {
        attributes.putAttribute(key, value);
    }
}
