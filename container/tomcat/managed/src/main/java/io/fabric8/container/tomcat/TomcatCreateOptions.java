/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package io.fabric8.container.tomcat;

import java.io.IOException;
import java.util.Properties;

import org.jboss.gravia.repository.MavenCoordinates;

import io.fabric8.spi.AbstractManagedCreateOptions;


public final class TomcatCreateOptions extends AbstractManagedCreateOptions {

    public static final String DEFAULT_JAVAVM_ARGUMENTS = "-Xmx512m -XX:MaxPermSize=128m";

    private int jmxPort = 8089;

    public int getJmxPort() {
        return jmxPort;
    }

    void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    @Override
    protected void validateConfiguration() {
        if (getMavenCoordinates().isEmpty()) {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream("version.properties"));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot load version.properties", ex);
            }
            String projectVersion = properties.getProperty("project.version");
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.poc", "fabric8-tomcat", projectVersion, "tar.gz", null));
        }
        if (getJavaVmArguments() == null) {
            setJavaVmArguments(DEFAULT_JAVAVM_ARGUMENTS);
        }
        super.validateConfiguration();
    }
}
