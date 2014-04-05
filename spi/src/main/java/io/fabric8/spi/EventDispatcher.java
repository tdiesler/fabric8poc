package io.fabric8.spi;

import io.fabric8.api.ComponentEvent;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;

public interface EventDispatcher {

    void dispatchProvisionEvent(ProvisionEvent event, ProvisionEventListener listener);

    void dispatchProfileEvent(ProfileEvent event, ProfileEventListener listener);

    void dispatchComponentEvent(ComponentEvent event);

}