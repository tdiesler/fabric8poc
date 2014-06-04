/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Managed
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
package io.fabric8.container.wildfly;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.process.ManagedCreateOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.resource.Version;


public final class WildFlyCreateOptions extends WildFlyProcessOptions implements ManagedCreateOptions {

    private final List<String> profiles = new ArrayList<>();
    private Version version = Version.emptyVersion;

    @Override
    public ContainerIdentity getIdentity() {
        return ContainerIdentity.create(getIdentityPrefix());
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public List<String> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }
}
