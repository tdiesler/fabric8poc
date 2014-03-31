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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.gravia.utils.NotNullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* The default implementation of a {@link PermitManager}
*
* @author thomas.diesler@jboss.com
* @since 05-Mar-2014
*/
public final class DefaultPermitManager implements PermitManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermitManager.class);
    private final Map<PermitState<?>, StatePermit<?>> permitmapping = new HashMap<PermitState<?>, StatePermit<?>>();

    @Override
    public <T> void activate(PermitState<T> state, T instance) {
        getStatePermit(state).activate(instance);
    }

    @Override
    public void deactivate(PermitState<?> state) {
        getStatePermit(state).deactivate(-1, null);
    }

    @Override
    public void deactivate(PermitState<?> state, long timeout, TimeUnit unit) {
        if (!getStatePermit(state).deactivate(timeout, unit)) {
            throw new PermitStateTimeoutException("Cannot deactivate state [" + state.getName() + "] in time", state, timeout, unit);
        }
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitState<T> state, boolean exclusive) {
        StatePermit<T> statePermit = getStatePermit(state);
        statePermit.acquire(exclusive, -1, null);
        return statePermit;
    }

    @Override
    public <T> Permit<T> aquirePermit(PermitState<T> state, boolean exclusive, long timeout, TimeUnit unit) {
        StatePermit<T> statePermit = getStatePermit(state);
        if (!statePermit.acquire(exclusive, timeout, unit)) {
            throw new PermitStateTimeoutException("Cannot aquire permit for state [" + state.getName() + "] in time", state, timeout, unit);
        }
        return statePermit;
    }

    @SuppressWarnings("unchecked")
    private <T> StatePermit<T> getStatePermit(PermitState<T> state) {
        NotNullException.assertValue(state, "state");
        synchronized (permitmapping) {
            StatePermit<?> statePermit = permitmapping.get(state);
            if (statePermit == null) {
                statePermit = new StatePermit<T>(state);
                permitmapping.put(state, statePermit);
            }
            return (StatePermit<T>) statePermit;
        }
    }

    static class StatePermit<T> implements Permit<T> {

        private final Semaphore clientPermits = new Semaphore(0);
        private final AtomicBoolean active = new AtomicBoolean();
        private final ExecutorService executor;
        private final PermitState<T> state;

        private CountDownLatch deactivationLatch;
        private boolean exclusiveLock;
        private int usageCount;
        private T activeInstance;

        StatePermit(PermitState<T> state) {
            this.state = state;
            this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable target) {
                    Thread thread = new Thread(target);
                    thread.setName("StateActivation");
                    return thread;
                }
            });
        }

        @Override
        public PermitState<T> getState() {
            return state;
        }

        @Override
        public T getInstance() {
            return activeInstance;
        }

        void activate(final T instance) {
            LOGGER.debug("activating: {}",  state);
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    if (active.compareAndSet(false, true)) {
                        activeInstance = instance;
                        if (!exclusiveLock) {
                            clientPermits.release(state.getMaximumPermits());
                        }
                        LOGGER.debug("activated: {}",  state);
                    } else {
                        LOGGER.debug("already active: {}",  state);
                    }
                }
            };
            try {
                executor.submit(task).get();
            } catch (InterruptedException ex) {
                // ignore
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Cannot activate state: " + state, ex.getCause());
            }
        }

        boolean acquire(boolean exclusive, long timeout, TimeUnit unit) {
            String timestr = unit != null ? " in " + unit.toMillis(timeout) + "ms" : "";
            String exclstr = exclusive ? " exclusive" : "";
            LOGGER.debug("aquiring" + exclstr + timestr + ": {}", state);

            boolean success;
            try {
                if (timeout > 0 && unit != null) {
                    success = clientPermits.tryAcquire(timeout, unit);
                } else {
                    clientPermits.acquire();
                    success = true;
                }
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }

            if (!success) {
                LOGGER.warn("Not aquired: {}", state);
                return false;
            }

            if (exclusive) {

                synchronized (this) {
                    clientPermits.drainPermits();
                    deactivationLatch = new CountDownLatch(usageCount);
                }

                long permitCount = deactivationLatch.getCount();
                LOGGER.debug("awaiting [{}] permits: {}", permitCount, state);

                // Wait for all permits to get returned
                try {
                    if (timeout > 0 && unit != null) {
                        success = deactivationLatch.await(timeout, unit);
                    } else {
                        deactivationLatch.await();
                        success = true;
                    }
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                } finally {
                    synchronized (this) {
                        deactivationLatch = null;
                    }
                }
            }

            if (success) {
                synchronized (this) {
                    exclusiveLock = exclusive;
                    usageCount++;
                }
                LOGGER.debug("aquired [" + usageCount + "]: {}", state);
            } else {
                LOGGER.warn("Not aquired: {}", state);
            }
            return success;
        }

        @Override
        public void release() {
            synchronized (this) {

                if (usageCount == 0) {
                    LOGGER.warn("State not in use: {}", state);
                    return;
                }

                usageCount--;

                if (deactivationLatch != null) {
                    deactivationLatch.countDown();
                } else {
                    if (exclusiveLock && usageCount == 0) {
                        exclusiveLock = false;
                        clientPermits.release(state.getMaximumPermits());
                    } else {
                        clientPermits.release();
                    }
                }
            }
            LOGGER.debug("released [" + usageCount + "]: {}", state);
        }

        boolean deactivate(final long timeout, final TimeUnit unit) {
            String timestr = unit != null ? " in " + unit.toMillis(timeout) + "ms" : "";
            LOGGER.debug("deactivating" + timestr + ": {}", state);

            Callable<Boolean> task = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    if (!active.get()) {
                        LOGGER.debug("already inactive: {}",  state);
                        return true;
                    }

                    synchronized (StatePermit.this) {
                        clientPermits.drainPermits();
                        deactivationLatch = new CountDownLatch(exclusiveLock ? 0 : usageCount);
                    }

                    long permitCount = deactivationLatch.getCount();
                    LOGGER.debug("awaiting [{}] permits: {}", permitCount, state);

                    try {
                        if (timeout > 0 && unit != null) {
                            return deactivationLatch.await(timeout, unit);
                        } else {
                            deactivationLatch.await();
                            return true;
                        }
                    } finally {
                        synchronized (StatePermit.this) {
                            deactivationLatch = null;
                            activeInstance = null;
                        }
                    }
                }
            };
            try {
                return deactivationResult(executor.submit(task).get());
            } catch (InterruptedException ex) {
                deactivationResult(false);
                throw new IllegalStateException(ex);
            } catch (ExecutionException ex) {
                deactivationResult(false);
                throw new IllegalStateException(ex.getCause());
            }
        }

        private boolean deactivationResult(boolean success) {
            if (success) {
                LOGGER.debug("deactivated: {}", state);
                active.set(false);
            } else {
                LOGGER.warn("Not deactivated: {}", state);
                restorePermits();
            }
            return success;
        }

        private void restorePermits() {
            synchronized (this) {
                int permits = state.getMaximumPermits() - usageCount;
                if (permits > 0) {
                    clientPermits.release(permits);
                }
            }
        }
    }
}
