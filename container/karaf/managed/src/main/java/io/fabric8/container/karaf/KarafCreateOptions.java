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
package io.fabric8.container.karaf;

import java.io.IOException;
import java.util.Properties;

import org.jboss.gravia.repository.MavenCoordinates;

import io.fabric8.spi.AbstractManagedCreateOptions;
import io.fabric8.spi.ContainerCreateHandler;


public final class KarafCreateOptions extends AbstractManagedCreateOptions {

    public static final String DEFAULT_JAVAVM_ARGUMENTS = "-Xmx512m";

    public static final int DEFAULT_RMI_SERVER_PORT = 44444;
    public static final int DEFAULT_RMI_REGISTRY_PORT = 1099;
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final int DEFAULT_HTTPS_PORT = 8443;

    private int rmiServerPort = DEFAULT_RMI_SERVER_PORT;
    private int rmiRegistryPort = DEFAULT_RMI_REGISTRY_PORT;
    private int httpPort = DEFAULT_HTTP_PORT;
    private int httpsPort = DEFAULT_HTTPS_PORT;

    KarafCreateOptions() {
        setIdentityPrefix("KarafManagedContainer");
    }

    @Override
    public Class<? extends ContainerCreateHandler> getPrimaryHandler() {
        return KarafContainerCreateHandler.class;
    }

    public int getRmiServerPort() {
        return rmiServerPort;
    }

    void setRmiServerPort(int rmiServerPort) {
        this.rmiServerPort = rmiServerPort;
    }

    public int getRmiRegistryPort() {
        return rmiRegistryPort;
    }

    void setRmiRegistryPort(int rmiRegistryPort) {
        this.rmiRegistryPort = rmiRegistryPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    @Override
    protected void validate() {
        if (getMavenCoordinates().isEmpty()) {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream("version.properties"));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot load version.properties", ex);
            }
            String projectVersion = properties.getProperty("project.version");
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.poc", "fabric8-karaf", projectVersion, "tar.gz", null));
        }
        if (getJavaVmArguments() == null) {
            setJavaVmArguments(DEFAULT_JAVAVM_ARGUMENTS);
        }
        super.validate();
    }
}
