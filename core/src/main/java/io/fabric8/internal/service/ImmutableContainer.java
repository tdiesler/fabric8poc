package io.fabric8.internal.service;

import io.fabric8.api.Container;

import org.jboss.gravia.utils.NotNullException;

final class ImmutableContainer implements Container {

    private final String name;
    private final State state;

    ImmutableContainer(ContainerState containerState) {
        NotNullException.assertValue(containerState, "containerState");
        this.name = containerState.getName();
        this.state = containerState.getState();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Container[name=" + name + ",state=" + state + "]";
    }
}