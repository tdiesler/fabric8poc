/*
 * #%L
 * Gravia :: Resolver
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
package io.fabric8.spi;

import java.io.Serializable;

import org.jboss.gravia.utils.IllegalArgumentAssertion;


/**
 * The agent identity
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 */
public final class AgentIdentity implements Serializable {

    private static final long serialVersionUID = -5725118775732949024L;

    private final String name;

    public static AgentIdentity create(String name) {
        return new AgentIdentity(name);
    }

    private AgentIdentity(String name) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AgentIdentity)) return false;
        AgentIdentity other = (AgentIdentity) obj;
        return other.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
