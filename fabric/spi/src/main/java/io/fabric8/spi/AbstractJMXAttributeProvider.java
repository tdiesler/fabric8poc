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
package io.fabric8.spi;

import io.fabric8.api.ContainerAttributes;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * An abstract JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class AbstractJMXAttributeProvider extends AttributeSupport implements JmxAttributeProvider {

    private final String jmxServerUrl;
    private final String jmxUsername;
    private final String jmxPassword;

    public AbstractJMXAttributeProvider(String jmxServerUrl, String jmxUsername, String jmxPassword) {
        IllegalArgumentAssertion.assertNotNull(jmxServerUrl, "jmxServerUrl");
        this.jmxServerUrl = jmxServerUrl;
        this.jmxUsername = jmxUsername;
        this.jmxPassword = jmxPassword;
        this.addAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerUrl);
    }

    @Override
    public String getJmxServerUrl() {
        return jmxServerUrl;
    }

    @Override
    public String getJmxUsername() {
        return jmxUsername;
    }

    @Override
    public String getJmxPassword() {
        return jmxPassword;
    }
}
