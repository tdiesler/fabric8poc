package io.fabric8.core.spi;

import io.fabric8.core.api.ComponentEvent;
import io.fabric8.core.api.ProfileEvent;
import io.fabric8.core.api.ProfileEventListener;
import io.fabric8.core.api.ProvisionEvent;
import io.fabric8.core.api.ProvisionEventListener;

public interface EventDispatcher {

    void dispatchProvisionEvent(ProvisionEvent event, ProvisionEventListener listener);

    void dispatchProfileEvent(ProfileEvent event, ProfileEventListener listener);

    void dispatchComponentEvent(ComponentEvent event);

}