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
package io.fabric8.core;

import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionListener;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceTracker;
import org.jboss.gravia.utils.NotNullException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { EventDispatcher.class }, immediate = true)
public final class EventDispatcher extends AbstractComponent {

    private final Set<ProvisionListener> listeners = new HashSet<ProvisionListener>();
    private ServiceTracker<?, ?> tracker;
    private ExecutorService executor;

    @Activate
    void activate() {
        executor = createExecutor();
        tracker = createTracker();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        executor.shutdown();
        tracker.close();
        deactivateComponent();
    }

    void dispatchEvent(final ProvisionEvent event, final ProvisionListener listener) {
        NotNullException.assertValue(event, "event");

        // Get a snapshot of the current listeners
        final HashSet<ProvisionListener> snapshot;
        synchronized (listeners) {
            snapshot = new HashSet<ProvisionListener>(listeners);
        }
        if (listener != null) {
            snapshot.add(listener);
        }

        // Submit the async event delivery to every listener
        for (final ProvisionListener aux : snapshot) {
            Runnable task = new Runnable() {
                public void run() {
                    aux.processEvent(event);
                }
            };
            executor.submit(task);
        }
    }

    private ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                return new Thread(run, EventDispatcher.class.getSimpleName()) {
                };
            }
        });
    }

    private ServiceTracker<?, ?> createTracker() {
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        ServiceTracker<?, ?> tracker = new ServiceTracker<ProvisionListener, ProvisionListener>(syscontext, ProvisionListener.class, null) {

            @Override
            public ProvisionListener addingService(ServiceReference<ProvisionListener> reference) {
                ProvisionListener service = super.addingService(reference);
                synchronized (listeners) {
                    listeners.add(service);
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference<ProvisionListener> reference, ProvisionListener service) {
                synchronized (listeners) {
                    listeners.remove(service);
                }
                super.removedService(reference, service);
            }
        };
        tracker.open();
        return tracker;
    }
}
