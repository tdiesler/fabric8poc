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
package io.fabric8.api;

import org.jboss.gravia.resource.Version;

/**
 * Fabric constants
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Constants {

    /**
     * The default profile version
     */
    Version DEFAULT_PROFILE_VERSION = Version.parseVersion("1.0");

    /**
     * The default profile name
     */
    ProfileIdentity DEFAULT_PROFILE_IDENTITY = ProfileIdentity.create("default");

    /**
     * The management domain
     */
    String MANAGEMENT_DOMAIN = "fabric8";

    /**
     * The attribute key for the Http port
     */
    AttributeKey<Integer> ATTRIBUTE_KEY_HTTP_PORT = AttributeKey.create("fabric8.http.port", Integer.class);
    /**
     * The attribute key for the Https port
     */
    AttributeKey<Integer> ATTRIBUTE_KEY_HTTPS_PORT = AttributeKey.create("fabric8.https.port", Integer.class);
    /**
     * The attribute key for JMX server URL
     */
    AttributeKey<String> ATTRIBUTE_KEY_JMX_SERVER_URL = AttributeKey.create("fabric8.jmx.server.url", String.class);
}
