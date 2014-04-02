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
package io.fabric8.spi.internal;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;

import java.io.InputStream;

import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.Version;

public final class DefaultProfileBuilder extends ProfileBuilder {

    private ProfileIdentity identity;

    @Override
    public ProfileBuilder addIdentity(String symbolicName, Version version) {
        this.identity = ProfileIdentity.create(symbolicName, version);
        return this;
    }

    @Override
    public ProfileBuilder importProfile(InputStream input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProfileBuilder addResources(Resource... resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Profile createProfile() {
        return new ProfileImpl(identity);
    }
}
