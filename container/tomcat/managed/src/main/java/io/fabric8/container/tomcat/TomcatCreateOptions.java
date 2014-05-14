/*
 * #%L
 * Fabric8 :: Container :: Tomcat :: Managed
 * %%
 * Copyright (C) 2014 Red Hat
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

import io.fabric8.spi.AbstractManagedCreateOptions;
import io.fabric8.spi.ContainerCreateHandler;

import java.io.IOException;
import java.util.Properties;

import org.jboss.gravia.resource.MavenCoordinates;


public final class TomcatCreateOptions extends AbstractManagedCreateOptions {

    public static final String DEFAULT_JAVAVM_ARGUMENTS = "-Xmx512m -XX:MaxPermSize=128m";

    public static final int DEFAULT_JMX_PORT = 8089;
    public static final int DEFAULT_AJP_PORT = 8009;
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final int DEFAULT_HTTPS_PORT = 8443;

    private int jmxPort = DEFAULT_JMX_PORT;
    private int ajpPort = DEFAULT_AJP_PORT;
    private int httpPort = DEFAULT_HTTP_PORT;
    private int httpsPort = DEFAULT_HTTPS_PORT;

    TomcatCreateOptions() {
        setIdentityPrefix("TomcatManagedContainer");
    }

    @Override
    public Class<? extends ContainerCreateHandler> getPrimaryHandler() {
        return TomcatContainerCreateHandler.class;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public int getAjpPort() {
        return ajpPort;
    }

    void setAjpPort(int ajpPort) {
        this.ajpPort = ajpPort;
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
            addMavenCoordinates(MavenCoordinates.create("io.fabric8.poc", "fabric8-tomcat", projectVersion, "tar.gz", null));
        }
        if (getJavaVmArguments() == null) {
            setJavaVmArguments(DEFAULT_JAVAVM_ARGUMENTS);
        }
        super.validate();
    }
}
