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
package org.jboss.fabric8.internal.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.fabric8.internal.scr.AbstractComponent;
import org.jboss.fabric8.services.Container;
import org.jboss.fabric8.services.FabricService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { FabricService.class }, configurationPid = FabricService.PID, immediate = true)
public final class FabricServiceImpl extends AbstractComponent implements FabricService {

    private static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    private final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    private final Map<String, Container> containers = new HashMap<String, Container>();
    private String prefix;

    @Activate
    void activate(Map<String, ?> config) {
        prefix = (String) config.get(Container.KEY_NAME_PREFIX);
        activateComponent();
    }

    // @Modified not implemented - we get a new compoennt with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getContainerPrefix() {
        assertValid();
        return prefix;
    }

    @Override
    public Container createContainer(String name) {
        assertValid();
        synchronized (containers) {
            if (containers.containsKey(name))
                throw new IllegalStateException("Container already exists: " + name);

            String prefixedName = prefix != null ? prefix + "." + name : name;
            MutableContainer container = new MutableContainer(prefixedName);
            containers.put(container.getName(), container);
            return container;
        }
    }

    @Override
    public Container getContainerByName(String name) {
        assertValid();
        synchronized (containers) {
            return containers.get(name);
        }
    }

    @Override
    public void startContainer(Container container) {
        assertValid();
        assertContainerExists(container.getName()).start();
    }

    @Override
    public void stopContainer(Container container) {
        assertValid();
        assertContainerExists(container.getName()).stop();
    }

    @Override
    public void destroyContainer(Container container) {
        assertValid();
        synchronized (containers) {
            Container removed = containers.remove(container.getName());
            MutableContainer.assertMutableContainer(removed).destroy();
        }
    }

    private MutableContainer assertContainerExists(String name) {
        synchronized (containers) {
            Container container = containers.get(name);
            return MutableContainer.assertMutableContainer(container);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
