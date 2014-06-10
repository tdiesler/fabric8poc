/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.core;

import io.fabric8.spi.scr.AbstractComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.resource.ResourceIdentity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(immediate = true)
@Service(CurrentContainerService.class)
public class CurrentContainerService extends AbstractComponent {

    private final Map<ResourceIdentity, ResourceHandle> resourceHandles = new LinkedHashMap<>();

    @Activate
    void activate() {
      activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    Map<ResourceIdentity, ResourceHandle> getResourceHandles() {
        return resourceHandles;
    }

    void addResourceHandles(Map<ResourceIdentity, ResourceHandle> handles) {
       resourceHandles.putAll(handles);
    }

    void removeResourceHandles(Collection<ResourceIdentity> handles) {
        for (ResourceIdentity resourceIdentity : handles) {
            resourceHandles.remove(resourceIdentity);
        }
    }
}
