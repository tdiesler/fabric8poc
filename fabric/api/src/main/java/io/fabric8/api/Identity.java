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
package io.fabric8.api;

import org.jboss.gravia.utils.NotNullException;


/**
 * An abstract string based identity
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
abstract class Identity {

    private final String symbolicName;

    Identity(String symbolicName) {
        NotNullException.assertValue(symbolicName, "symbolicName");
        this.symbolicName = symbolicName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public abstract String getCanonicalForm();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Identity)) return false;
        Identity other = (Identity) obj;
        return other.symbolicName.equals(symbolicName);
    }

    @Override
    public int hashCode() {
        return symbolicName.hashCode();
    }

    @Override
    public String toString() {
        return symbolicName;
    }
}
