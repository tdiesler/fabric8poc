/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
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

import io.fabric8.api.CreateOptions;
import io.fabric8.api.LifecycleException;
import io.fabric8.api.ServiceEndpoint;

import java.util.Set;


/**
 * A handle to a container instance
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerHandle {

    CreateOptions getCreateOptions();

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    void destroy() throws LifecycleException;

    Set<ServiceEndpoint> getServiceEndpoints();
}
