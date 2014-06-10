/*
 * #%L
 * Gravia :: Resolver
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
package io.fabric8.spi;

import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.process.ManagedProcess;
import io.fabric8.spi.process.ProcessIdentity;

import java.util.Set;
import java.util.concurrent.Future;

import javax.management.ObjectName;

import org.jboss.gravia.runtime.LifecycleException;
import org.jboss.gravia.utils.ObjectNameFactory;


/**
 * The agent interface
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
public interface Agent {

    ObjectName OBJECT_NAME = ObjectNameFactory.create("io.fabric8:type=Agent");

    String NOTIFICATION_TYPE_AGENT_REGISTRATION = "AgentRegistration";

    String NOTIFICATION_TYPE_AGENT_DEREGISTRATION = "AgentDeregistration";

    Set<String> getProcessHandlers();

    Set<ProcessIdentity> getProcessIdentities();

    ManagedProcess getManagedProcess(ProcessIdentity processId);

    ManagedProcess createProcess(ProcessOptions options);

    Future<ManagedProcess> startProcess(ProcessIdentity processId) throws LifecycleException;

    Future<ManagedProcess> stopProcess(ProcessIdentity processId) throws LifecycleException;

    ManagedProcess destroyProcess(ProcessIdentity processId);

    AgentTopology getAgentTopology();

    AgentTopology registerAgent(AgentRegistration agentReg);

    AgentTopology unregisterAgent(AgentIdentity agentId);
}
