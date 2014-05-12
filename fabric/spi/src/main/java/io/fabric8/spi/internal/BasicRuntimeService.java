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

import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.osgi.service.component.ComponentContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.Objects.requireNonNull;

@Component(immediate = true, metatype = true)
@Service(RuntimeService.class)
public class BasicRuntimeService extends AbstractComponent implements RuntimeService {

    static final String ENV_PREFIX = "env.prefix";
    static final String DEFAULT_ENV_PREFIX = "FABRIC8_";
    static final String REPLACE_PATTERN = "-|\\.";

    private final String id;
    private final Path home;
    private final Path data;
    private final Path conf;

    @Property(name = ENV_PREFIX, label = "Environment Variable Prefix", value = DEFAULT_ENV_PREFIX)
    private String envPrefix = DEFAULT_ENV_PREFIX;

    public BasicRuntimeService() {
        //These properties are not modifiable and need to be pre-configured.
        this.id = requireNonNull(getProperty(ID), "Runtime ID cannot be null.");
        this.home = Paths.get(requireNonNull(getProperty(HOME_DIR), "Runtime home directory cannot be null."));
        this.data = Paths.get(requireNonNull(getProperty(DATA_DIR), "Runtime data directory cannot be null."));
        this.conf = Paths.get(requireNonNull(getProperty(CONF_DIR), "Runtime conf directory cannot be null."));
    }

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        this.envPrefix = (String) componentContext.getProperties().get(ENV_PREFIX);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Path getHomePath() {
        return home;
    }

    @Override
    public Path getConfPath() {
        return conf;
    }

    @Override
    public Path getDataPath() {
        return data;
    }

    @Override
    public String getProperty(String key) {
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getPropertyInternal(key, defaultValue);
    }

    private synchronized String getPropertyInternal(String key, String defaultValue) {
        String value = String.valueOf(RuntimeLocator.getRequiredRuntime().getProperty(key));
        value = value != null ? value : System.getenv(toEnvVariable(envPrefix, key));
        return value != null ? value : defaultValue;
    }

    /**
     * Convert a system property name to an env variable name.
     * The convention is that env variables are prefixed with the specified prefix, capitalized and dots are converted to
     * underscores.
     * @param prefix    The prefix to use.
     * @param name      The system property name to convert.
     * @return          The corresponding env variable name.
     */
    private static String toEnvVariable(String prefix, String name) {
        if (name == null || name.isEmpty()) {
            return name;
        } else {
            return prefix + name.replaceAll(REPLACE_PATTERN,"_").toUpperCase();
        }
    }
}
