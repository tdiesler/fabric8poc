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

import io.fabric8.api.FabricEventListener;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private final Map<Class<?>, Set<FabricEventListener<?>>> listenerMapping = new HashMap<Class<?>, Set<FabricEventListener<?>>>();
    private ServiceTracker<ProvisionEventListener, ProvisionEventListener> provisionTracker;
    private ServiceTracker<ProfileEventListener, ProfileEventListener> profileTracker;
    private ExecutorService executor;

    @Activate
    void activate() {
        executor = Executors.newCachedThreadPool(getThreadFactory());
        provisionTracker = createTracker(ProvisionEventListener.class);
        profileTracker = createTracker(ProfileEventListener.class);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        executor.shutdown();
        provisionTracker.close();
        profileTracker.close();
        deactivateComponent();
    }

    void dispatchProvisionEvent(final ProvisionEvent event, final ProvisionEventListener listener) {
        NotNullException.assertValue(event, "event");
        Set<ProvisionEventListener> listeners = getEventListeners(ProvisionEventListener.class, listener);
        if (listeners != null) {
            for (final ProvisionEventListener aux : listeners) {
                Runnable task = new Runnable() {
                    public void run() {
                        aux.processEvent(event);
                    }
                };
                executor.submit(task);
            }
        }
    }

    void dispatchProfileEvent(final ProfileEvent event, final ProfileEventListener listener) {
        NotNullException.assertValue(event, "event");
        Set<ProfileEventListener> listeners = getEventListeners(ProfileEventListener.class, listener);
        if (listeners != null) {
            for (final ProfileEventListener aux : listeners) {
                Runnable task = new Runnable() {
                    public void run() {
                        aux.processEvent(event);
                    }
                };
                executor.submit(task);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends FabricEventListener<?>> Set<T> getEventListeners(Class<T> type, T listener) {
        Set<T> result = new HashSet<T>();
        synchronized (listenerMapping) {
            Set<T> snapshot = (Set<T>) listenerMapping.get(type);
            if (snapshot != null) {
                result.addAll(snapshot);
            }
        }
        if (listener != null) {
            result.add(listener);
        }
        return Collections.unmodifiableSet(result);
    }

    private ThreadFactory getThreadFactory() {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                return new Thread(run, EventDispatcher.class.getSimpleName());
            }
        };
        return threadFactory;
    }

    private <T extends FabricEventListener<?>> ServiceTracker<T, T> createTracker(final Class<T> type) {
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(syscontext, type, null) {

            @Override
            @SuppressWarnings("unchecked")
            public T addingService(ServiceReference<T> reference) {
                T service = super.addingService(reference);
                synchronized (listenerMapping) {
                    Set<T> listeners = (Set<T>) listenerMapping.get(type);
                    if (listeners == null) {
                        listeners = new HashSet<T>();
                        listenerMapping.put(type, (Set<FabricEventListener<?>>) listeners);
                    }
                    listeners.add(service);
                }
                return service;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void removedService(ServiceReference<T> reference, T service) {
                synchronized (listenerMapping) {
                    Set<T> listeners = (Set<T>) listenerMapping.get(type);
                    listeners.remove(service);
                }
                super.removedService(reference, service);
            }
        };
        tracker.open();
        return tracker;
    }
}
