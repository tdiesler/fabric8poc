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
package io.fabric8.core.internal;

import io.fabric8.api.ComponentEvent;
import io.fabric8.api.ComponentEventListener;
import io.fabric8.api.FabricEvent;
import io.fabric8.api.FabricEventListener;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.spi.EventDispatcher;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The event dispatcher
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(service = { EventDispatcher.class }, immediate = true)
public final class EventDispatcherImpl extends AbstractComponent implements EventDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDispatcherImpl.class);

    private final Map<Class<?>, Set<FabricEventListener<?>>> listenerMapping = new HashMap<Class<?>, Set<FabricEventListener<?>>>();
    private ServiceTracker<ProvisionEventListener, ProvisionEventListener> provisionTracker;
    private ServiceTracker<ProfileEventListener, ProfileEventListener> profileTracker;
    private ServiceTracker<ComponentEventListener, ComponentEventListener> componentTracker;
    private ExecutorService executor;

    @Activate
    void activate() {
        executor = Executors.newCachedThreadPool(getThreadFactory());
        componentTracker = createTracker(ComponentEventListener.class);
        provisionTracker = createTracker(ProvisionEventListener.class);
        profileTracker = createTracker(ProfileEventListener.class);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        executor.shutdown();
        componentTracker.close();
        provisionTracker.close();
        profileTracker.close();
        deactivateComponent();
    }

    @Override
    public void dispatchProvisionEvent(final ProvisionEvent event, final ProvisionEventListener listener) {
        dispatchEvent(event, ProvisionEventListener.class, listener);
    }

    @Override
    public void dispatchProfileEvent(final ProfileEvent event, final ProfileEventListener listener) {
        dispatchEvent(event, ProfileEventListener.class, listener);
    }

    @Override
    public void dispatchComponentEvent(final ComponentEvent event) {
        dispatchEvent(event, ComponentEventListener.class, null);
    }

    private <E extends FabricEvent<?, ?>, L extends FabricEventListener<E>> void dispatchEvent(final E event, Class<L> listenerType, L listener) {
        NotNullException.assertValue(event, "event");
        Set<L> listeners = getEventListeners(listenerType, listener);
        if (listeners != null) {
            for (final L aux : listeners) {
                Runnable task = new Runnable() {
                    public void run() {
                        try {
                            aux.processEvent(event);
                        } catch (Throwable th) {
                            LOGGER.error("Error delivering event: " + event, th);
                        }
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
                return new Thread(run, EventDispatcherImpl.class.getSimpleName());
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
