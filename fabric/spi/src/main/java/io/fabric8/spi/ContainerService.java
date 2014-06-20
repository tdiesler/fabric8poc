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

import io.fabric8.api.ContainerManager;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.spi.permit.PermitKey;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.gravia.provision.ProvisionException;

/**
 * The internal fabric service
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerService extends ContainerManager {

    /**
     * The {@link PermitKey} that protects this service.
     */
    PermitKey<ContainerService> PERMIT = new PermitKey<ContainerService>(ContainerService.class);

    /**
     * Get an url connection to content in the current container's effective profile
     *
     * Accepted URL are formated like
     *
     * container://[symbolicName]?version=[version]&cntindex=[contentIndex]
     *
     * The version parameters is optional. When missing it will use the higest version.
     */
    URLConnection getContainerURLConnection(URL url) throws IOException;

    /**
     * Update a profile in the current container
     *
     * [TODO] Review how updateProfile works with multiple changes in a ProfileVersion
     */
    void updateProfile(ProfileIdentity profile, ProvisionEventListener listener) throws ProvisionException;
}
