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

import io.fabric8.core.spi.permit.DefaultPermitManager;
import io.fabric8.core.spi.permit.PermitKey;
import io.fabric8.core.spi.permit.PermitManager;
import io.fabric8.core.spi.permit.PermitStateTimeoutException;
import io.fabric8.core.spi.permit.PermitManager.Permit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    PermitManager permitManager;

    @Before
    public void setUp() {
        permitManager = new DefaultPermitManager();
    }

    @Test
    public void testBasicLifecycle() throws Exception {
        PermitKey<StateA> stateA = new PermitKey<StateA>(StateA.class, "A");

        // No permit on inactive
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);

        // Activate
        permitManager.activate(stateA, new StateA());

        // Aquire permit
        Permit<StateA> permit = permitManager.aquirePermit(stateA, false);

        // Cannot deactivate while permits in use
        assertDeactivateTimeout(stateA, 100, TimeUnit.MILLISECONDS);
        permit.release();

        // Deactivate
        permitManager.deactivate(stateA, 100, TimeUnit.MILLISECONDS);

        // No permit on inactive
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testAquireExclusive() throws Exception {

        PermitKey<StateA> stateA = new PermitKey<StateA>(StateA.class, "A");

        permitManager.activate(stateA, new StateA());

        // Aquire exclusive permit
        Permit<StateA> permit = permitManager.aquirePermit(stateA, true);

        // Assert that no other permit can be obtained
        assertPermitTimeout(stateA, false, 100, TimeUnit.MILLISECONDS);
        assertPermitTimeout(stateA, true, 100, TimeUnit.MILLISECONDS);

        permit.release();
    }

    @Test
    public void testDeactivateWithExclusivePermit() throws Exception {

        final PermitKey<StateA> stateA = new PermitKey<StateA>(StateA.class, "A");

        StateA instanceA1 = new StateA();
        permitManager.activate(stateA, instanceA1);

        // Aquire exclusive permit
        Permit<StateA> permit = permitManager.aquirePermit(stateA, true);

        // Deactivate while holding an exclusive permit
        permitManager.deactivate(stateA, 100, TimeUnit.MILLISECONDS);

        StateA instanceA2 = new StateA();
        permitManager.activate(stateA, instanceA2);

        // Verify that another thread cannot aquire a permit
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger failures = new AtomicInteger();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    permitManager.aquirePermit(stateA, false, 100, TimeUnit.MILLISECONDS);
                } catch (PermitStateTimeoutException ex) {
                    failures.incrementAndGet();
                }
                try {
                    permitManager.aquirePermit(stateA, true, 100, TimeUnit.MILLISECONDS);
                } catch (PermitStateTimeoutException ex) {
                    failures.incrementAndGet();
                }
                latch.countDown();
            }
        }).start();

        Assert.assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        Assert.assertEquals("TimeoutException expected", 2, failures.get());

        permit.release();
    }

    private void assertPermitTimeout(PermitKey<?> state, boolean exclusive, long timeout, TimeUnit unit) {
        try {
            permitManager.aquirePermit(state, exclusive, timeout, unit);
            Assert.fail("TimeoutException expected");
        } catch (PermitStateTimeoutException ex) {
            // expected
        }
    }

    private void assertDeactivateTimeout(PermitKey<?> state, long timeout, TimeUnit unit) {
        try {
            permitManager.deactivate(state, timeout, unit);
            Assert.fail("TimeoutException expected");
        } catch (PermitStateTimeoutException ex) {
            // expected
        }
    }

    static class StateA {
    }
}