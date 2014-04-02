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

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;


/**
 * A profile identity
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class ProfileIdentity {

    private final String symbolicName;
    private final Version version;

    public static ProfileIdentity create(String symbolicName, Version version) {
        return new ProfileIdentity(symbolicName, version);
    }

    private ProfileIdentity(String symbolicName, Version version) {
        NotNullException.assertValue(symbolicName, "symbolicName");
        this.version = version != null ? version : Version.emptyVersion;
        this.symbolicName = symbolicName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProfileIdentity)) return false;
        ProfileIdentity other = (ProfileIdentity) obj;
        return other.symbolicName.equals(symbolicName) && other.version.equals(version);
    }

    @Override
    public int hashCode() {
        return (symbolicName + version).hashCode();
    }

    @Override
    public String toString() {
        return "[" + symbolicName + "-" + version + "]";
    }
}
