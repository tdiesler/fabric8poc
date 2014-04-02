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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;


/**
 * A builder for a profile version
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ProfileVersionBuilder {

    public static ProfileVersionBuilder create() {

        ProfileVersionBuilder builder = null;

        // First check if we have a {@link ProfileVersionBuilder} service
        Runtime runtime = RuntimeLocator.getRuntime();
        if (runtime != null) {
            builder = ServiceLocator.getService(ProfileVersionBuilder.class);
        }

        // Next use ServiceLoader discovery
        if (builder == null) {
            ClassLoader classLoader = ProfileVersionBuilder.class.getClassLoader();
            ServiceLoader<ProfileVersionBuilder> loader = ServiceLoader.load(ProfileVersionBuilder.class, classLoader);
            Iterator<ProfileVersionBuilder> iterator = loader.iterator();
            while (builder == null && iterator.hasNext()) {
                builder = iterator.next();
            }
        }

        if (builder == null)
            throw new IllegalStateException("Cannot obtain ProfileVersionBuilder service");

        return builder;
    }

    public abstract ProfileVersionBuilder addIdentity(Version version);

    public abstract ProfileVersion createProfileVersion();
}
