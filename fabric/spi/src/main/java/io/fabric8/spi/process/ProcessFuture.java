package io.fabric8.spi.process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ProcessFuture implements Future<ManagedProcess> {

    private final MutableManagedProcess process;
    private final CountDownLatch latch;

    public ProcessFuture(MutableManagedProcess process, CountDownLatch latch) {
        this.process = process;
        this.latch = latch;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public ManagedProcess get() {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return returnManagedProcess();
    }

    @Override
    public ManagedProcess get(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            if (latch.await(timeout, unit)) {
                return returnManagedProcess();
            } else {
                throw new TimeoutException();
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ManagedProcess returnManagedProcess() {
        return new ImmutableManagedProcess(process);
    }
}