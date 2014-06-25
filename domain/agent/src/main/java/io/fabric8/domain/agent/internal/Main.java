/*
 * #%L
 * Gravia :: Agent
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

import static io.fabric8.domain.agent.AgentLogger.LOGGER;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.embedded.spi.EmbeddedRuntimeFactory;
import org.jboss.gravia.runtime.spi.DefaultPropertiesProvider;
import org.jboss.gravia.runtime.spi.PropertiesHeadersProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;


/**
 * The gravia agent process.
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
public final class Main {

    private Runtime runtime;

    public static void main(String[] args) throws Exception {

        // Start the agent thread
        AgentRunner runner = new AgentRunner();
        runner.start();
    }

    public void start() throws Exception {

        // Create Runtime
        PropertiesProvider propsProvider = new DefaultPropertiesProvider();
        runtime = RuntimeLocator.createRuntime(new EmbeddedRuntimeFactory(), propsProvider);
        runtime.init();

        // Install/Start the Agent as a Module
        Module module = installAgentModule();
        module.start();
    }

    private Module installAgentModule() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("META-INF/agent-module.headers");
        Dictionary<String, String> headers = new PropertiesHeadersProvider(input).getHeaders();
        return runtime.installModule(classLoader, headers);
    }

    public boolean shutdown(long timeout, TimeUnit unit) {
        runtime.shutdown();
        try {
            return runtime.awaitShutdown(timeout, unit);
        } catch (InterruptedException ex) {
            return false;
        }
    }

    static class AgentRunner extends Thread {

        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private final AgentShutdown shutdownHook = new AgentShutdown(shutdownLatch);

        public AgentRunner() {
            setName("AgentThread");
            java.lang.Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        @Override
        public void run() {

            // Start the agent
            try {
                Main agent = new Main();
                agent.start();
            } catch (Exception ex) {
                LOGGER.error("Cannot start agent", ex);
            }

            // Wait for a call on the shutdown hook
            try {
                shutdownLatch.await();
            } catch (InterruptedException ex) {
                LOGGER.error("Agent thread interrupted", ex);
            }
        }
    }

    static class AgentShutdown extends Thread {

        private final CountDownLatch shutdownLatch;

        AgentShutdown(CountDownLatch shutdownLatch) {
            this.shutdownLatch = shutdownLatch;
            setName("AgentShutdown");
        }

        @Override
        public void run() {
            Runtime runtime = RuntimeLocator.getRequiredRuntime();
            LOGGER.info("Agent shutdown initiated ...");
            shutdownLatch.countDown();
            runtime.shutdown();
            try {
                if (runtime.awaitShutdown(10, TimeUnit.SECONDS)) {
                    LOGGER.info("Agent shutdown complete");
                } else {
                    LOGGER.error("Cannot shutdown agent in time");
                }
            } catch (InterruptedException ex) {
                LOGGER.error("Agent shutdown interrupted", ex);
            }
        }
    }
}
