/*
 * #%L
 * Fabric8 :: Container :: Karaf :: Managed
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

package io.fabric8.container.karaf;

import java.nio.file.Path;

import org.jboss.gravia.resource.MavenCoordinates;

import io.fabric8.spi.AbstractContainerBuilder;
import io.fabric8.api.process.ProcessBuilder;



/**
 * The {@link KarafCreateOptions} builder
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Apr-2014
 */
public final class KarafContainerBuilder extends AbstractContainerBuilder<KarafContainerBuilder, KarafCreateOptions> implements ProcessBuilder<KarafContainerBuilder, KarafCreateOptions> {

    public static KarafContainerBuilder create() {
        return new KarafContainerBuilder();
    }

    private KarafContainerBuilder() {
        super(new KarafCreateOptions());
    }

    public KarafContainerBuilder rmiServerPort(int serverPort) {
        options.setRmiServerPort(serverPort);
        return this;
    }

    public KarafContainerBuilder rmiRegistryPort(int registryPort) {
        options.setRmiRegistryPort(registryPort);
        return this;
    }

    public KarafContainerBuilder httpPort(int httpPort) {
        options.setHttpPort(httpPort);
        return this;
    }

    public KarafContainerBuilder httpsPort(int httpsPort) {
        options.setHttpsPort(httpsPort);
        return this;
    }

    @Override
    public KarafContainerBuilder identityPrefix(String prefix) {
        options.setIdentityPrefix(prefix);
        return this;
    }

    @Override
    public KarafContainerBuilder targetPath(Path targetPath) {
        options.setTargetPath(targetPath);
        return this;
    }

    @Override
    public KarafContainerBuilder jvmArguments(String javaVmArguments) {
        options.setJavaVmArguments(javaVmArguments);
        return this;
    }

    @Override
    public KarafContainerBuilder addMavenCoordinates(MavenCoordinates coordinates) {
        options.addMavenCoordinates(coordinates);
        return this;
    }

    @Override
    public KarafContainerBuilder outputToConsole(boolean outputToConsole) {
        options.setOutputToConsole(outputToConsole);
        return this;
    }

    @Override
    public KarafCreateOptions getProcessOptions() {
        options.validate();
        return options;
    }
}
