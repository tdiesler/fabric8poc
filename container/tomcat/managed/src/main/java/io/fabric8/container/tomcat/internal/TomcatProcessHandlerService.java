/*
 * #%L
 * Fabric8 :: Container :: Tomcat :: Managed
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

package io.fabric8.container.tomcat.internal;

import io.fabric8.api.process.MutableManagedProcess;
import io.fabric8.api.process.ProcessIdentity;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.container.tomcat.TomcatProcessHandler;
import io.fabric8.spi.process.ProcessHandler;
import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

/**
 * The Tomcat {@link ProcessHandler} service
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Jun-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProcessHandler.class)
public final class TomcatProcessHandlerService extends AbstractComponent implements ProcessHandler {

    private final TomcatProcessHandler delegate = new TomcatProcessHandler();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public boolean accept(ProcessOptions options) {
        assertValid();
        return delegate.accept(options);
    }

    public final MutableManagedProcess create(ProcessOptions options, ProcessIdentity identity) {
        assertValid();
        return delegate.create(options, identity);
    }

    public final void start(MutableManagedProcess process) {
        assertValid();
        delegate.start(process);
    }

    public final void stop(MutableManagedProcess process) {
        assertValid();
        delegate.stop(process);
    }

    @Override
    public void destroy(MutableManagedProcess process) {
        assertValid();
        delegate.destroy(process);
    }
}
