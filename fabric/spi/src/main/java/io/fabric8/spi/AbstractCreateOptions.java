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
import io.fabric8.api.Constants;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.VersionIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

public abstract class AbstractCreateOptions implements MutableCreateOptions {

    private final AttributeSupport attributes = new AttributeSupport();
    private final List<ProfileIdentity> profiles = new ArrayList<>();
    private VersionIdentity version = ProfileVersion.DEFAULT_PROFILE_VERSION;
    private ContainerIdentity identity;

    protected AbstractCreateOptions() {
        profiles.add(Constants.DEFAULT_PROFILE_IDENTITY);
    }

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
    }

    @Override
    public VersionIdentity getProfileVersion() {
        return version;
    }

    @Override
    public List<ProfileIdentity> getProfiles() {
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
    public <T> T getRequiredAttribute(AttributeKey<T> key) {
        return attributes.getRequiredAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    @Override
    public void setIdentity(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        this.identity = identity;
    }

    @Override
    public void setVersion(VersionIdentity version) {
        IllegalArgumentAssertion.assertNotNull(version, "version");
        this.version = version;
    }

    @Override
    public void setProfiles(List<ProfileIdentity> profiles) {
        IllegalArgumentAssertion.assertNotNull(profiles, "profiles");
        this.profiles.clear();
        this.profiles.addAll(profiles);
    }

    @Override
    public <T> void addAttribute(AttributeKey<T> key, T value) {
        attributes.addAttribute(key, value);
    }

    @Override
    public void addAttributes(Map<AttributeKey<?>, Object> atts) {
        attributes.addAttributes(atts);
    }

    @Override
    public void validate() {
        IllegalStateAssertion.assertNotNull(identity, "Identity cannot be null");
        IllegalStateAssertion.assertNotNull(version, "Profile version cannot be null");
    }
}
