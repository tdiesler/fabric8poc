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
package io.fabric8.container.karaf;

import io.fabric8.api.container.ManagedContainerBuilder;

import java.io.IOException;
import java.util.Properties;

import org.jboss.gravia.repository.MavenCoordinates;



/**
 * The managed container configuration builder
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class KarafManagedContainerBuilder extends ManagedContainerBuilder<KarafContainerConfiguration, KarafManagedContainer> {

    @Override
    protected KarafContainerConfiguration createConfiguration() {
        return new KarafContainerConfiguration();
    }

    @Override
    public KarafManagedContainer getManagedContainer() {
        return new KarafManagedContainer();
    }

    @Override
    protected void validateConfiguration(KarafContainerConfiguration config) {
        if (config.getMavenCoordinates().isEmpty()) {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream("version.properties"));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot load version.properties", ex);
            }
            String projectVersion = properties.getProperty("project.version");
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.poc", "fabric8-karaf", projectVersion, "tar.gz", null));
        }
        if (config.getJavaVmArguments() == null) {
            setJavaVmArguments(KarafContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS);
        }
        super.validateConfiguration(config);
    }
}
