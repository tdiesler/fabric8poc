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

package io.fabric8.spi;

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * A holder for the initial container Configuration.
 * This is intended to be used ONLY on first boot.
 * It provides Fabric8 related configuration that should be used to for registration purposes.
 * After that registration, its the responsibility of {@link io.fabric8.spi.ContainerService} to provide the actual config.
 */
public interface BootConfiguration {

    String VERSION = "version";
    String PROFILE = "profile";

    Version getVersion();
    Set<String> getProfiles();
    Map<String, Object> getConfiguration();
}
