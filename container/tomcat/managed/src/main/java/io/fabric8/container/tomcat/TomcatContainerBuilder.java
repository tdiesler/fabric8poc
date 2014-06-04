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

package io.fabric8.container.tomcat;

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
public final class TomcatContainerBuilder extends AbstractContainerBuilder<TomcatContainerBuilder, TomcatCreateOptions> implements ProcessBuilder<TomcatContainerBuilder, TomcatCreateOptions> {

    public static TomcatContainerBuilder create() {
        return new TomcatContainerBuilder();
    }

    private TomcatContainerBuilder() {
        super(new TomcatCreateOptions());
    }

    public TomcatContainerBuilder jmxPort(int jmxPort) {
        options.setJmxPort(jmxPort);
        return this;
    }

    public TomcatContainerBuilder ajpPort(int ajpPort) {
        options.setAjpPort(ajpPort);
        return this;
    }

    public TomcatContainerBuilder httpPort(int httpPort) {
        options.setHttpPort(httpPort);
        return this;
    }

    public TomcatContainerBuilder httpsPort(int httpsPort) {
        options.setHttpsPort(httpsPort);
        return this;
    }

    @Override
    public TomcatContainerBuilder identityPrefix(String prefix) {
        options.setIdentityPrefix(prefix);
        return this;
    }

    @Override
    public TomcatContainerBuilder targetPath(Path targetPath) {
        options.setTargetPath(targetPath);
        return this;
    }

    @Override
    public TomcatContainerBuilder jvmArguments(String javaVmArguments) {
        options.setJavaVmArguments(javaVmArguments);
        return this;
    }

    @Override
    public TomcatContainerBuilder addMavenCoordinates(MavenCoordinates coordinates) {
        options.addMavenCoordinates(coordinates);
        return this;
    }

    @Override
    public TomcatContainerBuilder outputToConsole(boolean outputToConsole) {
        options.setOutputToConsole(outputToConsole);
        return this;
    }

    @Override
    public TomcatCreateOptions getProcessOptions() {
        options.validate();
        return options;
    }
}
