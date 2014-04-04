/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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

import java.util.EventObject;

import org.jboss.gravia.utils.NotNullException;


/**
 * A provisioning event
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
@SuppressWarnings("serial")
public class ProvisionEvent extends EventObject {

    private final EventType type;
    private final Profile profile;
    private final Throwable error;

    public enum EventType {
        PROVISIONING, PROVISIONED, REMOVING, REMOVED, ERROR
    }

    public ProvisionEvent(Container source, Profile profile, EventType type) {
        this(source, profile, type, null);
    }

    public ProvisionEvent(Container source, Profile profile, EventType type, Throwable error) {
        super(source);
        NotNullException.assertValue(profile, "profile");
        NotNullException.assertValue(type, "type");
        this.profile = profile;
        this.type = type;
        this.error = error;
    }

    @Override
    public Container getSource() {
        return (Container) super.getSource();
    }

    public EventType getType() {
        return type;
    }

    public Profile getProfile() {
        return profile;
    }

    public Throwable getError() {
        return error;
    }
}
