/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Managed
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

package io.fabric8.container.wildfly;

import io.fabric8.api.process.ProcessBuilder;
import io.fabric8.spi.AbstractContainerBuilder;

import java.net.InetAddress;
import java.nio.file.Path;

import org.jboss.gravia.resource.MavenCoordinates;



/**
 * The {@link WildFlyCreateOptions} builder
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Apr-2014
 */
public final class WildFlyContainerBuilder extends AbstractContainerBuilder<WildFlyContainerBuilder, WildFlyCreateOptions> implements ProcessBuilder<WildFlyContainerBuilder, WildFlyCreateOptions> {

    public static WildFlyContainerBuilder create() {
        return new WildFlyContainerBuilder();
    }

    private WildFlyContainerBuilder() {
        super(new WildFlyCreateOptions());
    }

    @Override
    public WildFlyContainerBuilder targetHost(InetAddress targetHost) {
        options.setTargetHost(targetHost);
        return this;
    }

    public WildFlyContainerBuilder serverConfig(String serverConfig) {
        options.setServerConfig(serverConfig);
        return this;
    }

    public WildFlyContainerBuilder managementNativePort(int nativePort) {
        options.setManagementNativePort(nativePort);
        return this;
    }

    public WildFlyContainerBuilder managementHttpPort(int httpPort) {
        options.setManagementHttpPort(httpPort);
        return this;
    }

    public WildFlyContainerBuilder managementHttpsPort(int httpsPort) {
        options.setManagementHttpsPort(httpsPort);
        return this;
    }

    public WildFlyContainerBuilder ajpPort(int ajpPort) {
        options.setAjpPort(ajpPort);
        return this;
    }

    public WildFlyContainerBuilder httpPort(int httpPort) {
        options.setHttpPort(httpPort);
        return this;
    }

    public WildFlyContainerBuilder httpsPort(int httpsPort) {
        options.setHttpsPort(httpsPort);
        return this;
    }

    @Override
    public WildFlyContainerBuilder identityPrefix(String prefix) {
        options.setIdentityPrefix(prefix);
        return this;
    }

    @Override
    public WildFlyContainerBuilder targetPath(Path targetPath) {
        options.setTargetPath(targetPath);
        return this;
    }

    @Override
    public WildFlyContainerBuilder jvmArguments(String javaVmArguments) {
        options.setJavaVmArguments(javaVmArguments);
        return this;
    }

    @Override
    public WildFlyContainerBuilder addMavenCoordinates(MavenCoordinates coordinates) {
        options.addMavenCoordinates(coordinates);
        return this;
    }

    @Override
    public WildFlyContainerBuilder outputToConsole(boolean outputToConsole) {
        options.setOutputToConsole(outputToConsole);
        return this;
    }

    @Override
    public WildFlyCreateOptions getProcessOptions() {
        options.validate();
        return options;
    }
}
