/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.wildfly;

import io.fabric8.api.container.ContainerConfiguration;

/**
 * The WildFly container configuration
 *
 * @since 26-Feb-2014
 */
public final class WildFlyContainerConfiguration extends ContainerConfiguration {

    public static final String DEFAULT_SERVER_CONFIG = "standalone-fabric.xml";
    public static final String DEFAULT_JAVAVM_ARGUMENTS = "-Xmx1024m";

    private String serverConfig = WildFlyContainerConfiguration.DEFAULT_SERVER_CONFIG;

    public String getServerConfig() {
        return serverConfig;
    }

    void setServerConfig(String serverConfig) {
        assertMutable();
        this.serverConfig = serverConfig;
    }
}