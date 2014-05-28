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

import static java.util.Objects.requireNonNull;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.osgi.service.component.ComponentContext;

@Component(immediate = true)
@Service(RuntimeService.class)
public class BasicRuntimeService extends AbstractComponent implements RuntimeService {

    private final String id;
    private final Path home;
    private final Path data;
    private final Path conf;


    public BasicRuntimeService() {
        //These properties are not modifiable and need to be pre-configured.
        this.id = requireNonNull(getProperty(ID), "Runtime ID cannot be null.");
        this.home = Paths.get(requireNonNull(getProperty(HOME_DIR), "Runtime home directory cannot be null."));
        this.data = Paths.get(requireNonNull(getProperty(DATA_DIR), "Runtime data directory cannot be null."));
        this.conf = Paths.get(requireNonNull(getProperty(CONF_DIR), "Runtime conf directory cannot be null."));
    }

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
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
        Object raw = RuntimeLocator.getRequiredRuntime().getProperty(key);
        if (raw == null) {
            return defaultValue;
        } else {
            return String.valueOf(raw);
        }
    }
}
