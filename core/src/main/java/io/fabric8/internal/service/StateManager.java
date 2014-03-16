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
package io.fabric8.internal.service;

import io.fabric8.api.state.State;
import io.fabric8.api.state.StateService;
import io.fabric8.api.state.StateTimeoutException;
import io.fabric8.internal.scr.AbstractComponent;
import io.fabric8.spi.DefaultStateService;

import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { StateService.class }, immediate = true)
public final class StateManager extends AbstractComponent implements StateService {

    private final StateService delegate = new DefaultStateService();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public <T> void activate(State<T> state, T instance) {
        assertValid();
        delegate.activate(state, instance);
    }

    @Override
    public void deactivate(State<?> state) {
        assertValid();
        delegate.deactivate(state);
    }

    @Override
    public void deactivate(State<?> state, long timeout, TimeUnit unit) throws StateTimeoutException {
        assertValid();
        delegate.deactivate(state, timeout, unit);
    }

    @Override
    public <T> Permit<T> aquirePermit(State<T> state, boolean exclusive) {
        assertValid();
        return delegate.aquirePermit(state, exclusive);
    }

    @Override
    public <T> Permit<T> aquirePermit(State<T> state, boolean exclusive, long timeout, TimeUnit unit) throws StateTimeoutException {
        assertValid();
        return delegate.aquirePermit(state, exclusive, timeout, unit);
    }

}
