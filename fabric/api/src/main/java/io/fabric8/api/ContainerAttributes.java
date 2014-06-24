/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.api;


public final class ContainerAttributes  {

    // Hide ctor
    private ContainerAttributes() {
    }

    /**
     * The attribute key for the Advertised IP Address
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_IP = AttributeKey.create("fabric8.ip", String.class);

    /**
     * The attribute key for the Advertised IP Address
     */
    public static final  AttributeKey<String> ATTRIBUTE_ADDRESS_RESOLVER = AttributeKey.create("fabric8.address.resolver", String.class);

    /**
     * The attribute key for the Bind Address
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_BIND_ADDRESS = AttributeKey.create("fabric8.bind.address", String.class);

    /**
     * The attribute key for the Local IP
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_LOCAL_IP = AttributeKey.create("fabric8.bind.local.ip", String.class);

    /**
     * The attribute key for the Host Name
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_HOSTNAME = AttributeKey.create("fabric8.hostname", String.class);

    /**
     * The attribute key for the Fully Qualified Name
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_FQN = AttributeKey.create("fabric8.fqn", String.class);

    /**
     * The attribute key for the Alias (an alternative address that is manually specified)
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_ALIAS = AttributeKey.create("fabric8.alias", String.class);

    /**
     * The attribute key for the Http port
     */
    public static final  AttributeKey<Integer> ATTRIBUTE_KEY_HTTP_PORT = AttributeKey.create("fabric8.http.port", Integer.class);

    /**
     * The attribute key for the Https port
     */
    public static final  AttributeKey<Integer> ATTRIBUTE_KEY_HTTPS_PORT = AttributeKey.create("fabric8.https.port", Integer.class);

    /**
     * The attribute key for the Http URL
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_HTTP_URL = AttributeKey.create("fabric8.http.url", String.class);

    /**
     * The attribute key for the Http URL
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_HTTPS_URL = AttributeKey.create("fabric8.https.url", String.class);

    /**
     * The attribute key for JMX server URL
     */
    public static final AttributeKey<String> ATTRIBUTE_KEY_JMX_SERVER_URL = AttributeKey.create("fabric8.jmx.server.url", String.class);

    /**
     * The attribute key for Jolokia Agent URL that created the remote process
     */
    public static final AttributeKey<String> ATTRIBUTE_KEY_REMOTE_AGENT_URL = AttributeKey.create("fabric8.jolokia.agent.url", String.class);

    /**
     * The attribute key for SSH server URL
     */
    public static final AttributeKey<String> ATTRIBUTE_KEY_SSH_SERVER_URL = AttributeKey.create("fabric8.ssh.server.url", String.class);
}
