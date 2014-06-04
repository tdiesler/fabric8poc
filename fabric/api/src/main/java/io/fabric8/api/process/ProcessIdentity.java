/*
 * #%L
 * Fabric8 :: API
 * %%
 * Copyright (C) 2014 Red Hat
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
package io.fabric8.api.process;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * A process identity
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class ProcessIdentity  {

    private final String name;

    public static ProcessIdentity create(String name) {
        return new ProcessIdentity(name);
    }

    private ProcessIdentity(String name) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProcessIdentity)) return false;
        ProcessIdentity other = (ProcessIdentity) obj;
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
