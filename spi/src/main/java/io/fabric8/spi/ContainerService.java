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
package io.fabric8.spi;

import io.fabric8.api.ContainerManager;
import io.fabric8.spi.permit.PermitState;

/**
 * The internal fabric service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerService extends ContainerManager {

    /**
     * A key into the configuration that names the container name prefix created by the current service instance
     */
    String KEY_NAME_PREFIX = "container.prefix";

    /**
     * The configuration PID for this service
     */
    String CONTAINER_SERVICE_PID = "container.service.pid";

    /**
     * The {@link PermitState} that protects this service.
     */
    PermitState<ContainerService> PERMIT = new PermitState<ContainerService>(ContainerService.class);
}
