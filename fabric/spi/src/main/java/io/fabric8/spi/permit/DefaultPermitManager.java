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
package io.fabric8.spi.permit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jboss.gravia.utils.NotNullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* The default implementation of a {@link PermitManager}
*
* @author Thomas.Diesler@jboss.com
* @since 05-Mar-2014
*/
public final class DefaultPermitManager implements PermitManager {

    public static long DEFAULT_TIMEOUT = 60000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermitManager.class);
    private final Map<PermitKey<?>, PermitState<?>> permits = new HashMap<PermitKey<?>, PermitState<?>>();

    @Override
    public <T> void activate(PermitKey<T> key, T instance) {
        getPermitState(key).activate(instance);
    }

    @Override
    public void deactivate(PermitKey<?> key) {
        getPermitState(key).deactivate(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void deactivate(PermitKey<?> key, long timeout, TimeUnit unit) {
        getPermitState(key).deactivate(timeout, unit);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive) {
        return getPermitState(key).acquire(exclusive, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive, long timeout, TimeUnit unit) {
        return getPermitState(key).acquire(exclusive, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    private <T> PermitState<T> getPermitState(PermitKey<T> key) {
        NotNullException.assertValue(key, "key");
        synchronized (permits) {
            PermitState<?> permitState = permits.get(key);
            if (permitState == null) {
                permitState = new PermitState<T>(key);
                permits.put(key, permitState);
            }
            return (PermitState<T>) permitState;
        }
    }

    static class PermitState<T> {

        private final Semaphore semaphore = new Semaphore(0);
        private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock(true);
        private final AtomicReference<CountDownLatch> deactivationLatch = new AtomicReference<CountDownLatch>();
        private final AtomicReference<T> activeInstance = new AtomicReference<T>();
        private final AtomicBoolean exclusiveLock = new AtomicBoolean();
        private final AtomicInteger usageCount = new AtomicInteger();
        private final AtomicBoolean active = new AtomicBoolean();
        private final PermitKey<T> key;

        PermitState(PermitKey<T> key) {
            this.key = key;
        }

        void activate(T instance) {
            if (!active.compareAndSet(false, true))
                throw new IllegalStateException("Cannot activate an already active state");

            LOGGER.debug("activating: {}",  key);

            deactivationLatch.set(new CountDownLatch(0));
            activeInstance.set(instance);
            exclusiveLock.set(false);
            semaphore.release(1);
        }

        Permit<T> acquire(boolean exclusive, long timeout, TimeUnit unit) {

            final String timestr = unit != null ? " in " + unit.toMillis(timeout) + "ms" : "";
            final String exclstr = exclusive ? " exclusive" : "";
            LOGGER.debug("aquiring" + exclstr + timestr + ": {}", key);

            getSinglePermit(timeout, unit);

            final Lock lock;
            if (exclusive) {
                lock = writeLock(timeout, unit);
                exclusiveLock.set(true);
            } else {
                lock = readLock(timeout, unit);
                usageCount.incrementAndGet();
                semaphore.release(1);
            }

            LOGGER.debug("aquired" + exclstr + ": {}", key);

            return new Permit<T>() {

                @Override
                public PermitKey<T> getPermitKey() {
                    return key;
                }

                @Override
                public T getInstance() {
                    return activeInstance.get();
                }

                @Override
                public void release() {
                    LOGGER.debug("releasing" + exclstr + ": {}", key);
                    deactivationLatch.get().countDown();
                    int usage = usageCount.decrementAndGet();
                    if (usage > 0) {
                        LOGGER.debug("remaining: {} => [{}]", key, usage);
                    }
                    lock.unlock();
                }
            };
        }

        void deactivate(long timeout, TimeUnit unit) {

            LOGGER.debug("deactivating: {}",  key);

            if (!active.get()) {
                LOGGER.debug("not active: {}",  key);
                return;
            }

            if (exclusiveLock.get()) {
                LOGGER.debug("deactivated: {}",  key);
                active.set(false);
                return;
            }

            getSinglePermit(timeout, unit);

            boolean success;
            try {
                int usage = usageCount.get();
                deactivationLatch.set(new CountDownLatch(usage));
                LOGGER.debug("waiting: {} => [{}]",  key, usage);
                success = deactivationLatch.get().await(timeout, unit);
            } catch (InterruptedException ex) {
                success = false;
            }

            if (success) {
                LOGGER.debug("deactivated: {}",  key);
                active.set(false);
            } else {
                semaphore.release(1);
                throw new PermitStateTimeoutException("Cannot deactivate state [" + key.getName() + "] in time", key, timeout, unit);
            }
        }

        private void getSinglePermit(long timeout, TimeUnit unit) {
            try {
                if (!semaphore.tryAcquire(timeout, unit)) {
                    throw new PermitStateTimeoutException("Cannot aquire permit for [" + key.getName() + "] in time", key, timeout, unit);
                }
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private ReadLock readLock(long timeout, TimeUnit unit) {
            ReadLock lock = rwlock.readLock();
            try {
                if (!lock.tryLock() && !lock.tryLock(timeout, unit))
                    throw new PermitStateTimeoutException("Cannot aquire read lock for [" + key.getName() + "] in time", key, timeout, unit);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            return lock;
        }

        private WriteLock writeLock(long timeout, TimeUnit unit) {
            WriteLock lock = rwlock.writeLock();
            try {
                if (!lock.tryLock() && !lock.tryLock(timeout, unit))
                    throw new PermitStateTimeoutException("Cannot aquire write lock for [" + key.getName() + "] in time", key, timeout, unit);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            return lock;
        }
    }
}
