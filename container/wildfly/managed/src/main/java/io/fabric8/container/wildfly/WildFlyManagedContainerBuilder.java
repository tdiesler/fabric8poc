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

import io.fabric8.api.container.ManagedContainerBuilder;

import java.io.IOException;
import java.util.Properties;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.runtime.RuntimeType;

/**
 * The WildFly managed container builder
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class WildFlyManagedContainerBuilder extends ManagedContainerBuilder<WildFlyContainerConfiguration, WildFlyManagedContainer> {

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.WILDFLY;
    }

    @Override
    public WildFlyManagedContainer getManagedContainer() {
        return new WildFlyManagedContainer();
    }

    @Override
    protected WildFlyContainerConfiguration createConfiguration() {
        return new WildFlyContainerConfiguration();
    }

    public void setServerConfig(String serverConfig) {
        configuration.setServerConfig(serverConfig);
    }

    @Override
    protected void validateConfiguration(WildFlyContainerConfiguration config) {
        if (config.getMavenCoordinates().isEmpty()) {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream("version.properties"));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot load version.properties", ex);
            }
            String wildflyVersion = properties.getProperty("wildfly.version");
            String projectVersion = properties.getProperty("project.version");
            addMavenCoordinates(MavenCoordinates.create("org.wildfly", "wildfly-dist", wildflyVersion, "zip", null));
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.poc", "fabric8-container-wildfly-patch", projectVersion, "zip", null));
        }
        if (config.getJavaVmArguments() == null) {
            setJavaVmArguments(WildFlyContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS);
        }
        super.validateConfiguration(config);
    }
}
