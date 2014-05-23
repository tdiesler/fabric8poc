/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Managed
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

package io.fabric8.container.wildfly;

import io.fabric8.spi.AbstractManagedContainerBuilder;

/**
 * The WildFly managed container builder
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class WildFlyContainerBuilder extends AbstractManagedContainerBuilder<WildFlyContainerBuilder, WildFlyCreateOptions> {

    public static WildFlyContainerBuilder create() {
        return new WildFlyContainerBuilder();
    }

    private WildFlyContainerBuilder() {
        super(new WildFlyCreateOptions());
    }

    @Override
    public WildFlyManagedContainer getManagedContainer() {
        return new WildFlyManagedContainer(getCreateOptions());
    }

    public WildFlyContainerBuilder serverConfig(String serverConfig) {
        options.setServerConfig(serverConfig);
        return this;
    }

    public WildFlyContainerBuilder managementNativePort(int nativePort) {
        options.setManagementNativePort(nativePort);
        return this;
    }

    public WildFlyContainerBuilder managementHttpPort(int httpPort) {
        options.setManagementHttpPort(httpPort);
        return this;
    }

    public WildFlyContainerBuilder managementHttpsPort(int httpsPort) {
        options.setManagementHttpsPort(httpsPort);
        return this;
    }

    public WildFlyContainerBuilder ajpPort(int ajpPort) {
        options.setAjpPort(ajpPort);
        return this;
    }

    public WildFlyContainerBuilder httpPort(int httpPort) {
        options.setHttpPort(httpPort);
        return this;
    }

    public WildFlyContainerBuilder httpsPort(int httpsPort) {
        options.setHttpsPort(httpsPort);
        return this;
    }
}
