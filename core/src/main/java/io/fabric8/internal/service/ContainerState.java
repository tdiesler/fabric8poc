package io.fabric8.internal.service;

import io.fabric8.api.Container.State;

import java.util.concurrent.atomic.AtomicReference;

final class ContainerState {

    private final String name;
    private final AtomicReference<State> state = new AtomicReference<State>();

    ContainerState(String name) {
        this.name = name;
        this.state.set(State.CREATED);
    }

    String getName() {
        return name;
    }

    State getState() {
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