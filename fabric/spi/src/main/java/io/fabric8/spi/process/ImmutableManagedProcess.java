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

package io.fabric8.spi.process;

import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.AttributeSupport;

import java.nio.file.Path;

import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * An immutable managed process
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public final class ImmutableManagedProcess extends AttributeSupport implements ManagedProcess {

    private final ProcessIdentity identity;
    private final ProcessOptions options;
    private final Path homePath;
    private final State state;

    public ImmutableManagedProcess(ProcessIdentity identity, ProcessOptions options, Path homePath, State state) {
        super(options.getAttributes(), true);
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        IllegalArgumentAssertion.assertNotNull(homePath, "homePath");
        IllegalArgumentAssertion.assertNotNull(state, "state");
        this.identity = identity;
        this.options = options;
        this.homePath = homePath;
        this.state = state;
    }

    public ImmutableManagedProcess(ManagedProcess process) {
        super(process.getAttributes(), true);
        this.identity = process.getIdentity();
        this.options = process.getCreateOptions();
        this.homePath = process.getHomePath();
        this.state = process.getState();
    }

    @Override
    public ProcessIdentity getIdentity() {
        return identity;
    }

    @Override
    public ProcessOptions getCreateOptions() {
        return options;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return options.getRuntimeType();
    }

    @Override
    public Path getHomePath() {
        return homePath;
    }

    @Override
    public State getState() {
        return state;
    }
}
