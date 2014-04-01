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

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceReference;



/**
 * A builder for a fabric container
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ContainerBuilder {

    @SuppressWarnings("unchecked")
    public static <T extends ContainerBuilder> T create(Class<T> type) {

        T builder = null;

        // First check if we have a {@link ContainerBuilder} service
        Runtime runtime = RuntimeLocator.getRuntime();
        if (runtime != null) {
            ModuleContext sysontext = runtime.getModuleContext();
            ServiceReference<ContainerBuilder> sref = sysontext.getServiceReference(ContainerBuilder.class);
            builder = (T) (sref != null ? sysontext.getService(sref) : null);
        }

        // Next use ServiceLoader discovery
        if (builder == null) {
            ClassLoader classLoader = ContainerBuilder.class.getClassLoader();
            ServiceLoader<ContainerBuilder> loader = ServiceLoader.load(ContainerBuilder.class, classLoader);
            Iterator<ContainerBuilder> iterator = loader.iterator();
            while (builder == null && iterator.hasNext()) {
                ContainerBuilder auxcb = iterator.next();
                if (type.isAssignableFrom(auxcb.getClass())) {
                    builder = (T) auxcb;
                }
            }
        }

        if (builder == null)
            throw new IllegalStateException("Cannot obtain ContainerBuilder service for: " + type);

        return builder;
    }

    public abstract ContainerBuilder setRuntimeType(RuntimeType type);

    public abstract ContainerBuilder addIdentity(String name);

    public abstract ContainerBuilder setHost(Host host);

    public abstract Container createContainer();
}
