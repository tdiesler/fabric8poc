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



/**
 * A provisioning event
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
@SuppressWarnings("serial")
public class ProvisionEvent extends FabricEvent<Container, ProvisionEvent.Type> {

    public enum Type {
        PROVISIONING, PROVISIONED, REMOVING, REMOVED, ERROR
    }

    public ProvisionEvent(Container source, Type type, Profile profile) {
        this(source, type, profile, null);
    }

    public ProvisionEvent(Container source, Type type, Profile profile, Throwable error) {
        super(source, type, profile, error);
    }

    @Override
    public String toString() {
        return "ProvisionEvent[source=" + getSource().getIdentity() + ",profile=" + getProfile().getIdentity() + ",type=" + getType() + "]";
    }
}
