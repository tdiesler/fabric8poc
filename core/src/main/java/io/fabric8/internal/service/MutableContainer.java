package io.fabric8.internal.service;

import io.fabric8.api.services.Container;

import java.util.concurrent.atomic.AtomicReference;

final class MutableContainer implements Container {

    private final String name;
    private final AtomicReference<State> state = new AtomicReference<Container.State>();

    MutableContainer(String name) {
        this.name = name;
        this.state.set(State.CREATED);
    }

    static MutableContainer assertMutableContainer(Container container) {
        if (!(container instanceof MutableContainer))
            throw new IllegalArgumentException("Not a mutable container: " + container);
        return (MutableContainer) container;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State getState() {
        return state.get();
    }

    void start() {
        synchronized (state) {
            assertNotDestroyed();
            state.set(State.STARTED);
        }
    }

    void stop() {
        synchronized (state) {
            assertNotDestroyed();
            state.set(State.STOPPED);
        }
    }

    void destroy() {
        synchronized (state) {
            assertNotDestroyed();
            state.set(State.DESTROYED);
        }
    }

    private void assertNotDestroyed() {
        if (state.get() == State.DESTROYED)
            throw new IllegalStateException("Container already destroyed: " + this);
    }

    @Override
    public String toString() {
        return "Container[name=" + name + ",state=" + state.get() + "]";
    }
}