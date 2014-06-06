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

import io.fabric8.api.Attributable;
import io.fabric8.api.AttributeKey;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.AgentRegistration;

import java.nio.file.Path;


/**
 * The managed root container
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public interface ManagedProcess extends Attributable {

    /**
     * The attribute key for the {@link AgentRegistration}
     */
    AttributeKey<AgentRegistration> ATTRIBUTE_KEY_AGENT_REGISTRATION = AttributeKey.create("fabric8.agent.registration", AgentRegistration.class);

    enum State {
        CREATED, STARTED, STOPPED, DESTROYED
    }

    ProcessOptions getCreateOptions();

    ProcessIdentity getIdentity();

    Path getHomePath();

    State getState();
}
