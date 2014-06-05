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
package io.fabric8.domain.agent.internal;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.process.ManagedProcess;
import io.fabric8.api.process.ProcessIdentity;
import io.fabric8.domain.agent.Agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The agent servlet
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
@SuppressWarnings("serial")
public final class AgentServlet extends HttpServlet {

    private final Agent agent;

    AgentServlet(Agent agent) {
        this.agent = agent;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        writer.println("Agent");
        writer.println("=====");
        writer.println();

        writer.println("Process Handlers");
        writer.println("----------------");
        for (String handler : agent.getProcessHandlers()) {
            writer.println("   " + handler);
        }
        writer.println();

        writer.println("Processes");
        writer.println("---------");
        for (ProcessIdentity procid : agent.getProcessIdentities()) {
            ManagedProcess process = agent.getManagedProcess(procid);
            writer.println("   ProcessIdentity: " + process.getIdentity());
            writer.println("   State: " + process.getState());
            writer.println("   Attributes:");
            for (Entry<AttributeKey<?>, Object> entry : process.getAttributes().entrySet()) {
                writer.println("      " + entry.getKey() + "=" + entry.getValue());
            }
            writer.println();
        }
        writer.println();
    }
}