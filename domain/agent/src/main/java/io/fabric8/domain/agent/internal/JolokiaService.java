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

import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jolokia.osgi.JolokiaActivator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The Jolokia service
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Jun-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(JolokiaService.class)
public final class JolokiaService extends AbstractComponent {

    private final BundleActivator activator = new JolokiaActivator();
    private BundleContext context;

    @Activate
    void activate(BundleContext context) throws Exception {
        this.context = context;
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateInternal();
        deactivateComponent();
    }

    private void activateInternal() throws Exception {
        // Jolokia assumes a TCL in various places
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JolokiaService.class.getClassLoader());
            activator.start(context);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private void deactivateInternal() throws Exception {
        activator.stop(context);
    }
}
