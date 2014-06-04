/*
 * #%L
 * Fabric8 :: SPI
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
package io.fabric8.spi.process;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.process.ProcessBuilder;

import java.nio.file.Path;
import java.util.Map;

import org.jboss.gravia.resource.MavenCoordinates;

public abstract class AbstractProcessBuilder<B extends ProcessBuilder<B, T>, T extends AbstractProcessOptions> implements ProcessBuilder<B, T> {

    protected final T options;

    protected AbstractProcessBuilder(T options) {
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B identityPrefix(String prefix) {
        options.setIdentityPrefix(prefix);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addMavenCoordinates(MavenCoordinates coordinates) {
        options.addMavenCoordinates(coordinates);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B targetPath(Path targetPath) {
        options.setTargetPath(targetPath);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B jvmArguments(String javaVmArguments) {
        options.setJavaVmArguments(javaVmArguments);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B outputToConsole(boolean outputToConsole) {
        options.setOutputToConsole(outputToConsole);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> B addAttribute(AttributeKey<V> key, V value) {
        options.addAttribute(key, value);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addAttributes(Map<AttributeKey<?>, Object> atts) {
        options.addAttributes(atts);
        return (B) this;
    }

    @Override
    public T getProcessOptions() {
        options.validate();
        return options;
    }
}
