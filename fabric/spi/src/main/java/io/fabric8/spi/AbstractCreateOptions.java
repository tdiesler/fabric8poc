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

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;

import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

public abstract class AbstractCreateOptions implements CreateOptions {

    private ContainerIdentity identity;

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
    }

    protected void validate() {
        IllegalStateAssertion.assertNotNull(identity, "Identity cannot be null");
    }

    // Setters are protected

    protected void setIdentity(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        this.identity = identity;
    }
}
