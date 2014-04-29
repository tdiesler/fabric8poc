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

import org.jboss.gravia.utils.NotNullException;



/**
 * A service endpoint identity
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class ServiceEndpointIdentity<T extends ServiceEndpoint> extends Identity {

    private final Class<T> type;
    private final String canonicalForm;

    public static <T extends ServiceEndpoint> ServiceEndpointIdentity<T> create(String symbolicName, Class<T> type) {
        return new ServiceEndpointIdentity<T>(symbolicName, type);
    }

    private ServiceEndpointIdentity(String symbolicName, Class<T> type) {
        super(symbolicName);
        NotNullException.assertValue(type, "type");
        this.type = type;
        this.canonicalForm = "[name=" + getSymbolicName() + ",type=" + type.getName() + "]";
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public String getCanonicalForm() {
        return canonicalForm;
    }

    @Override
    public String toString() {
        return "ServiceEndpoint" + canonicalForm;
    }
}
