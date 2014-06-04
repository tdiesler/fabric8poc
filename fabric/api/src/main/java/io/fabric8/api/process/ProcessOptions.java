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

import io.fabric8.api.Attributable;
import io.fabric8.api.AttributeKey;

import java.nio.file.Path;
import java.util.List;

import org.jboss.gravia.resource.MavenCoordinates;

/**
 * Process create options
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProcessOptions extends Attributable {

    /**
     * The attribute key for the Http port
     */
    AttributeKey<Integer> ATTRIBUTE_KEY_HTTP_PORT = AttributeKey.create("gravia.http.port", Integer.class);
    /**
     * The attribute key for the Https port
     */
    AttributeKey<Integer> ATTRIBUTE_KEY_HTTPS_PORT = AttributeKey.create("gravia.https.port", Integer.class);
    /**
     * The attribute key for JMX server URL
     */
    AttributeKey<String> ATTRIBUTE_KEY_JMX_SERVER_URL = AttributeKey.create("gravia.jmx.server.url", String.class);

    String getIdentityPrefix();

    List<MavenCoordinates> getMavenCoordinates();

    Path getTargetPath();

    String getJavaVmArguments();

    boolean isOutputToConsole();
}
