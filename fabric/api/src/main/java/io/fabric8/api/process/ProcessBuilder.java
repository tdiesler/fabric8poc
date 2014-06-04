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
package io.fabric8.api.process;

import io.fabric8.api.AttributeKey;

import java.nio.file.Path;
import java.util.Map;

import org.jboss.gravia.resource.MavenCoordinates;

/**
 * Builder for {@link ProcessOptions}
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProcessBuilder<B extends ProcessBuilder<B, C>, C extends ProcessOptions> {

    B identityPrefix(String prefix);

    B targetPath(Path targetPath);

    B jvmArguments(String javaVmArguments);

    B addMavenCoordinates(MavenCoordinates coordinates);

    B outputToConsole(boolean outputToConsole);

    <V> B addAttribute(AttributeKey<V> key, V value);

    B addAttributes(Map<AttributeKey<?>, Object> attributes);

    C getProcessOptions();
}
