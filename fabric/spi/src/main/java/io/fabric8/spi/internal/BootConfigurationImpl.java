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

package io.fabric8.spi.internal;

import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.BootConfiguration;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IllegalStateAssertion;

@Component(configurationPid = "io.fabric8.boot", policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(BootConfiguration.class)
public class BootConfigurationImpl extends AbstractComponent implements BootConfiguration {

    @Reference
    Configurer configurer;

    private final Map<String, Object> configuration = new HashMap<>();
    private final Set<ProfileIdentity> profiles = new HashSet<>();
    private VersionIdentity version;

    @Activate
    void activate(Map<String, Object> source) throws Exception {
        this.configuration.putAll(configurer.configure(source, this));
        IllegalStateAssertion.assertTrue(configuration.containsKey(VERSION), VERSION + " is required");
        IllegalStateAssertion.assertTrue(configuration.containsKey(PROFILE), PROFILE + " is required");
        version = VersionIdentity.createFrom(String.valueOf(configuration.get(VERSION)));
        for (String prfid : String.valueOf(configuration.get(PROFILE)).split(" +")) {
            profiles.add(ProfileIdentity.createFrom(prfid));
        }
        activateComponent();
    }

    void deactivate() {
        deactivateComponent();
    }

    @Override
    public VersionIdentity getVersion() {
        return version;
    }

    @Override
    public Set<ProfileIdentity> getProfiles() {
        return Collections.unmodifiableSet(profiles);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
    }

    void bindConfigurer(Configurer configurer){
        this.configurer=configurer;
    }
    void unbindConfigurer(Configurer configurer) {
        this.configurer=null;
    }
}
