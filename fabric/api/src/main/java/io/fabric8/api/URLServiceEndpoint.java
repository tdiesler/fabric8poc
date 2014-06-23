/*
 * #%L
 * Fabric8 :: API
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
package io.fabric8.api;


/**
 * A URL service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jun-2014
 */
public interface URLServiceEndpoint extends ServiceEndpoint {

    /**
     * The attribute key for the service URL
     */
    AttributeKey<String> ATTRIBUTE_KEY_SERVICE_URL = AttributeKey.create("fabric8.service.url", String.class);
    /**
     * Jolokia {@link ServiceEndpoint} identity
     */
    public static ServiceEndpointIdentity JOLOKIA_SERVICE_ENDPOINT_IDENTITY = ServiceEndpointIdentity.create("jolokia");
    /**
     * JMX {@link ServiceEndpoint} identity
     */
    public static ServiceEndpointIdentity JMX_SERVICE_ENDPOINT_IDENTITY = ServiceEndpointIdentity.create("jmx");
    /**
     * JMX {@link ServiceEndpoint} identity
     */
    public static ServiceEndpointIdentity HTTP_SERVICE_ENDPOINT_IDENTITY = ServiceEndpointIdentity.create("http");

    String getServiceURL();
}
