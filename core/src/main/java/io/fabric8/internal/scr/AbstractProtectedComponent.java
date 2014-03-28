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
package io.fabric8.internal.scr;

import io.fabric8.internal.api.PermitManager;
import io.fabric8.internal.api.PermitState;

public abstract class AbstractProtectedComponent<T> extends AbstractComponent {

    private final ValidatingReference<PermitManager> stateService = new ValidatingReference<PermitManager>();

    protected void activateComponent(PermitState<T> state, T instance) {
        super.activateComponent();
        stateService.get().activate(state, instance);
    }

    protected void deactivateComponent(PermitState<T> state) {
        stateService.get().deactivate(state);
        super.deactivateComponent();
    }

    protected final void deactivateComponent() {
        throw new UnsupportedOperationException();
    }

    protected void bindPermitManager(PermitManager stateService) {
        this.stateService.bind(stateService);
    }
    protected void unbindPermitManager(PermitManager stateService) {
        this.stateService.unbind(stateService);
    }
}
