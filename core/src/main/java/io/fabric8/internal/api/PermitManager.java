/*
 * #%L
 * Gravia :: Runtime :: API
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
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
package io.fabric8.internal.api;

import java.util.concurrent.TimeUnit;

/**
* A service that allows controlled transitions from one state to another.
*
* A {@link PermitState} is associated with a number of permits that a client can obtain.
* To begin a state transition, all activity on a given state must be completed (i.e. all permits returned)
* During a state transition it is not possible to aquire a state permit.
* When a state transition ends the maximum set of state permits is restored.
*
* @author thomas.diesler@jboss.com
* @since 05-Mar-2014
*/
public interface PermitManager {

    /**
     * Activate the given state
     */
    <T> void activate(PermitState<T> state, T instance);

    /**
     * Deactivate the given state.
     *
     * This method blocks until all permits for the given state are returned.
     * No new permits can be aquired while the given state is in transition.
     */
    void deactivate(PermitState<?> state);

    /**
     * Deactivate the given state.
     *
     * This method blocks until all permits for the given state are returned.
     * No new permits can be aquired while the given state is in transition.
     *
     * @throws PermitStateTimeoutException if the given timeout was reached before all permits were returned
     */
    void deactivate(PermitState<?> state, long timeout, TimeUnit unit) throws PermitStateTimeoutException;

    /**
     * Aquire an exclusive permit for the given state.
     *
     * This method blocks until a permit on the given state is available.
     */
    <T> Permit<T> aquirePermit(PermitState<T> state, boolean exclusive);

    /**
     * Aquire a permit for the given state.
     *
     * This method blocks until a permit on the given state is available.
     *
     * @throws PermitStateTimeoutException if the given timeout was reached before a permit became available
     */
    <T> Permit<T> aquirePermit(PermitState<T> state, boolean exclusive, long timeout, TimeUnit unit) throws PermitStateTimeoutException;

    interface Permit<T> {

        /**
         * Get the state associated with this permit
         */
        PermitState<T> getState();

        /**
         * Get the instance associated with this permit
         */
        T getInstance();

        /**
         * Releaes this permit
         */
        void release();
    }
}