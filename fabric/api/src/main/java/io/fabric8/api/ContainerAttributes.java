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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerAttributes  {

    public static final String TYPE = "container.attributes";
    private static final String CONTAINER_ATTRIBUTE_FORMAT = "${container:%s/%s}";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{container:([a-zA-Z0-9\\.\\-]+)/([a-zA-Z0-9\\.\\-]+)}");

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
    public static final  AttributeKey<String> ATTRIBUTE_ALIAS = AttributeKey.create("fabric8.alias", String.class);

    /**
     * The attribute key for the Http port
     */
    public static final  AttributeKey<Integer> ATTRIBUTE_KEY_HTTP_PORT = AttributeKey.create("fabric8.http.port", Integer.class);
    /**
     * The attribute key for the Https port
     */
    public static final  AttributeKey<Integer> ATTRIBUTE_KEY_HTTPS_PORT = AttributeKey.create("fabric8.https.port", Integer.class);

    /**
     * The attribute key for JMX server URL
     */
    public static final  AttributeKey<String> ATTRIBUTE_KEY_HTTP_URL = AttributeKey.create("fabric8.http.url", String.class);

    /**
     * The attribute key for JMX server URL
     */
    public static final AttributeKey<String> ATTRIBUTE_KEY_JMX_SERVER_URL = AttributeKey.create("fabric8.jmx.server.url", String.class);

    /**
     * The attribute key for SSH server URL
     */
    public static final AttributeKey<String> ATTRIBUTE_KEY_SSH_SERVER_URL = AttributeKey.create("fabric8.ssh.server.url", String.class);

    public static final  Map<AttributeKey<?>, Object> substituteContainerAttributes(Map<AttributeKey<?>, Object> attributes, Set<AttributeKey<?>> visited) {
        Map<AttributeKey<?>, Object> result = new HashMap<>();
        for ( Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            AttributeKey<?> key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            String substitutedValue = substituteContainerAttribute(value, attributes, new HashSet<AttributeKey<?>>());
            result.put(key, key.getFactory().createFrom(substitutedValue));

        }
        return result;
    }

    private static final String substituteContainerAttribute(String str, Map<AttributeKey<?>, Object> attributes, Set<AttributeKey<?>> visited) {
        String result = str;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(str);
        CopyOnWriteArraySet<AttributeKey<?>> copyOfVisited = new CopyOnWriteArraySet<>(visited);
        while (matcher.find()) {
            String containerName = matcher.group(1);
            String attributeName = matcher.group(2);
            String replacement = "";
            String toReplace = String.format(CONTAINER_ATTRIBUTE_FORMAT, containerName, attributeName);
            AttributeKey attributeKey = AttributeKey.create(attributeName, Object.class);
            if (attributes.containsKey(attributeKey) && !visited.contains(attributeKey)) {
                replacement = String.valueOf(attributes.get(attributeKey));
                replacement = replacement != null ? replacement : "";
                if (PLACEHOLDER_PATTERN.matcher(replacement).matches()) {
                    copyOfVisited.add(attributeKey);
                    replacement = substituteContainerAttribute(replacement, attributes, copyOfVisited);
                }
            }
            result = result.replaceAll(toReplace, Matcher.quoteReplacement(replacement));
        }
        return result;
    }
}
