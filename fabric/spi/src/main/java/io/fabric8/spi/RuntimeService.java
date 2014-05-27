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


import java.nio.file.Path;

public interface RuntimeService {

    String ID = "runtime.id";
    String HOME_DIR = "runtime.home";
    String DATA_DIR = "runtime.data";
    String CONF_DIR = "runtime.conf";

    String DEFAULT_ENV_PREFIX = "FABRIC8_";

    /**
     * @return The unqiue runtime id.
     */
    String getId();

    /**
     * @return the home path of the current runtime.
     */
    Path getHomePath();

    /**
     * @return the conf path of the current runtime.
     */
    Path getConfPath();

    /**
     * @return the data path of the current runtime.
     */
    Path getDataPath();

    String getProperty(String key);

    String getProperty(String key, String defaultValue);
}
