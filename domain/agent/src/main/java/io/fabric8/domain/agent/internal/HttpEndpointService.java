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

import static io.fabric8.domain.agent.internal.AgentLogger.LOGGER;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceTracker;
import org.osgi.service.http.HttpService;

/**
 * The agent Http endpoint service
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
public final class HttpEndpointService {

    @Reference(referenceInterface = Agent.class)
    private final ValidatingReference<Agent> agent = new ValidatingReference<>();
    private ServiceTracker<?, ?> httpTracker;

    @Activate
    void activate(Map<String, Object> config) {
        activateInternal();
    }

    @Deactivate
    void deactivate() {
        if (httpTracker != null) {
            httpTracker.close();
        }
    }

    private void activateInternal() {
        ModuleContext syscontext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        httpTracker = new ServiceTracker<HttpService, HttpService>(syscontext, HttpService.class.getName(), null) {

            public HttpService addingService(ServiceReference<HttpService> sref) {
                HttpService service = super.addingService(sref);
                try {
                    service.registerServlet("/agent", new AgentServlet(agent.get()), null, null);
                    // [TODO] compute actual endpoint url
                    LOGGER.info("Agent HttpEndpoint registered: http://localhost:8080/agent");
                } catch (Exception ex) {
                    LOGGER.error("Cannot register agent servlet", ex);
                }
                return service;
            }

            public void removedService(ServiceReference<HttpService> sref, HttpService service) {
                service.unregister("/agent");
                LOGGER.info("Agent HttpEndpoint unregistered");
            }
        };
        httpTracker.open();
    }

    void bindAgent(Agent service) {
        agent.bind(service);
    }
    void unbindAgent(Agent service) {
        agent.unbind(service);
    }
}
