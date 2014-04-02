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

import org.jboss.gravia.utils.NotNullException;


/**
 * A host identity
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class HostIdentity {

    private final String symbolicName;

    public static HostIdentity create(String symbolicNamen) {
        return new HostIdentity(symbolicNamen);
    }

    private HostIdentity(String symbolicName) {
        NotNullException.assertValue(symbolicName, "symbolicName");
        this.symbolicName = symbolicName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HostIdentity)) return false;
        HostIdentity other = (HostIdentity) obj;
        return other.symbolicName.equals(symbolicName);
    }

    @Override
    public int hashCode() {
        return symbolicName.hashCode();
    }

    @Override
    public String toString() {
        return getSymbolicName();
    }
}
