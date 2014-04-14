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
package io.fabric8.api.container;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.gravia.repository.MavenCoordinates;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.utils.NotNullException;

/**
 * The managed container configuration builder
 *
 * @since 26-Feb-2014
 */
public abstract class ManagedContainerBuilder<C extends ContainerConfiguration, T extends ManagedContainer<C>> {

    protected C configuration;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends ManagedContainerBuilder<?, ?>> T create(Class<T> type) {
        ManagedContainerBuilder builder = null;

        // First check if we have a {@link ManagedContainerBuilder} service
        Runtime runtime = RuntimeLocator.getRuntime();
        if (runtime != null) {
            ModuleContext sysontext = runtime.getModuleContext();
            ServiceReference<ManagedContainerBuilder> sref = sysontext.getServiceReference(ManagedContainerBuilder.class);
            builder = sref != null ? sysontext.getService(sref) : null;
        }

        // Next use ServiceLoader discovery
        if (builder == null) {
            ClassLoader classLoader = ManagedContainerBuilder.class.getClassLoader();
            ServiceLoader<ManagedContainerBuilder> loader = ServiceLoader.load(ManagedContainerBuilder.class, classLoader);
            Iterator<ManagedContainerBuilder> iterator = loader.iterator();
            while (builder == null && iterator.hasNext()) {
                ManagedContainerBuilder mcb = iterator.next();
                if (type.isAssignableFrom(mcb.getClass())) {
                    builder = mcb;
                }
            }
        }

        if (builder == null)
            throw new IllegalStateException("Cannot obtain ManagedContainerBuilder service for: " + type);

        builder.configuration = builder.createConfiguration();
        return (T) builder;
    }

    public abstract T getManagedContainer();

    protected abstract C createConfiguration();

    public ManagedContainerBuilder<C, T> addMavenCoordinates(MavenCoordinates coordinates) {
        configuration.addMavenCoordinates(coordinates);
        return this;
    }

    public ManagedContainerBuilder<C, T> setTargetDirectory(String target) {
        configuration.setTargetDirectory(new File(target).getAbsoluteFile());
        return this;
    }

    public ManagedContainerBuilder<C, T> setJavaVmArguments(String javaVmArguments) {
        configuration.setJavaVmArguments(javaVmArguments);
        return this;
    }

    public ManagedContainerBuilder<C, T> setPortOffset(int portOffset) {
        configuration.setPortOffset(portOffset);
        return this;
    }

    public ManagedContainerBuilder<C, T> setOutputToConsole(boolean outputToConsole) {
        configuration.setOutputToConsole(outputToConsole);
        return this;
    }

    protected void validateConfiguration(C config) {
        NotNullException.assertValue(config.getMavenCoordinates(), "mavenCoordinates");
        NotNullException.assertValue(config.getTargetDirectory(), "targetDirectory");
        config.makeImmutable();
    }

    public final C getConfiguration() {
        validateConfiguration(configuration);
        return configuration;
    }
}
