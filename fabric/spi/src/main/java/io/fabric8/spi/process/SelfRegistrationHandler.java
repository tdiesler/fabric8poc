/*
 * #%L
 * Gravia :: Resolver
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
package io.fabric8.spi.process;

import io.fabric8.api.process.ManagedProcess.State;
import io.fabric8.api.process.MutableManagedProcess;
import io.fabric8.api.process.ProcessIdentity;
import io.fabric8.api.process.ProcessOptions;

import org.jboss.gravia.runtime.LifecycleException;

/**
 * The self registration handler
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
public class SelfRegistrationHandler implements ProcessHandler {

    @Override
    public boolean accept(ProcessOptions options) {
        return options instanceof SelfRegistrationOptions;
    }

    @Override
    public MutableManagedProcess create(ProcessOptions options, ProcessIdentity identity) {
        return new DefaultManagedProcess(identity, options, options.getTargetPath(), State.STARTED);
    }

    @Override
    public void start(MutableManagedProcess process) throws LifecycleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(MutableManagedProcess process) throws LifecycleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(MutableManagedProcess process) {
        throw new UnsupportedOperationException();
    }
}
