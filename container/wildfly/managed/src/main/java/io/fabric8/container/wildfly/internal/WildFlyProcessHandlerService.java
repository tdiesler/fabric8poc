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

package io.fabric8.container.wildfly.internal;

import io.fabric8.api.process.ProcessOptions;
import io.fabric8.container.wildfly.WildFlyProcessHandler;
import io.fabric8.container.wildfly.WildFlyProcessOptions;
import io.fabric8.spi.process.ProcessHandlerFactory;
import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

/**
 * The WildFly {@link ProcessHandlerFactory} service
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Jun-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProcessHandlerFactory.class)
public final class WildFlyProcessHandlerService extends AbstractComponent implements ProcessHandlerFactory {

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public WildFlyProcessHandler accept(ProcessOptions options) {
        assertValid();
        if (options instanceof WildFlyProcessOptions) {
            return new WildFlyProcessHandler();
        } else {
            return null;
        }
    }
}
