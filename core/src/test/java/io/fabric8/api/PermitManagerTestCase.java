/*
 * #%L
 * Gravia :: Runtime :: Embedded
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
package io.fabric8.api;

import io.fabric8.internal.api.PermitManager;
import io.fabric8.internal.api.State;
import io.fabric8.internal.api.StateTimeoutException;
import io.fabric8.internal.api.PermitManager.Permit;
import io.fabric8.internal.service.DefaultPermitManager;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link PermitManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 */
public class PermitManagerTestCase {

    PermitManager stateService;

    @Before
    public void setUp() {
        stateService = new DefaultPermitManager();
    }

    @Test
    public void testBasicLifecycle() throws Exception {
        State<StateA> stateA = new State<StateA>(StateA.class, "A", 1);

        // No permit on inactive state
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);

        // Activate the state
        stateService.activate(stateA, new StateA());

        // Aquire max permits
        Permit<StateA> permit = stateService.aquirePermit(stateA, false);
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);

        // Cannot deactivate while permits in use
        assertDeactivateTimeout(stateA, 100, TimeUnit.MILLISECONDS);
        permit.release();

        // Deactivate state
        stateService.deactivate(stateA, 100, TimeUnit.MILLISECONDS);

        // No permit on inactive state
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReleaseFromOtherThread() throws Exception {

        State<StateA> stateA = new State<StateA>(StateA.class, "A", 1);

        stateService.activate(stateA, new StateA());

        final Permit<StateA> permit = stateService.aquirePermit(stateA, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore
                }
                permit.release();
            }
        }).start();

        stateService.deactivate(stateA, 500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testAquireExclusive() throws Exception {

        State<StateA> stateA = new State<StateA>(StateA.class, "A", 2);

        stateService.activate(stateA, new StateA());

        // Aquire exclusive permit
        Permit<StateA> permit = stateService.aquirePermit(stateA, true);

        // Assert that no other permit can be obtained
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);
        assertPermitTimeout(stateA, true, 100, TimeUnit.MILLISECONDS);

        permit.release();
        stateService.aquirePermit(stateA, false);
    }

    @Test
    public void testDeactivateWithExclusivePermit() throws Exception {

        State<StateA> stateA = new State<StateA>(StateA.class, "A", 2);

        StateA instanceA1 = new StateA();
        stateService.activate(stateA, instanceA1);

        // Aquire all permits
        Permit<StateA> permit = stateService.aquirePermit(stateA, true);

        // Deactivate while holding an exclusive permit
        stateService.deactivate(stateA, 100, TimeUnit.MILLISECONDS);

        StateA instanceA2 = new StateA();
        stateService.activate(stateA, instanceA2);

        // Assert that no other permit can be obtained
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);

        permit.release();
        stateService.aquirePermit(stateA, false);
    }

    @Test
    public void testMaxPermits() throws Exception {

        State<StateA> stateA = new State<StateA>(StateA.class, "A", 2);

        stateService.activate(stateA, new StateA());

        // Aquire all permits
        stateService.aquirePermit(stateA, false);
        Permit<StateA> permit = stateService.aquirePermit(stateA, false);

        // Assert that no other permit can be obtained
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);

        permit.release();
        stateService.aquirePermit(stateA, false);
    }

    private void assertPermitTimeout(State<?> state, boolean exclusive, long timeout, TimeUnit unit) {
        try {
            stateService.aquirePermit(state, exclusive, timeout, unit);
            Assert.fail("TimeoutException expected");
        } catch (StateTimeoutException ex) {
            // expected
        }
    }

    private void assertDeactivateTimeout(State<?> state, long timeout, TimeUnit unit) {
        try {
            stateService.deactivate(state, timeout, unit);
            Assert.fail("TimeoutException expected");
        } catch (StateTimeoutException ex) {
            // expected
        }
    }

    static class StateA {
    }
}