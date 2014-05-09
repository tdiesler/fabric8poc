/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.api;

import java.io.InputStream;
import java.util.Map;

import org.jboss.gravia.resource.Version;

public interface ProfileBuilderBase<B extends ProfileBuilderBase<B>> extends AttributableBuilder<B> {

    B identity(String identity);

    B profileVersion(Version version);

    B addProfileItem(ProfileItem item);

    B removeProfileItem(String identity);

    B addConfigurationItem(String identity, Map<String, Object> config);

    ConfigurationItemBuilder<B> withConfigurationItem(String identity);

    B addResourceItem(String identity, InputStream inputStream);

    ResourceItemBuilder<B> withResourceItem(String identity);

    B addParentProfile(String identity);

    B removeParentProfile(String identity);
}
