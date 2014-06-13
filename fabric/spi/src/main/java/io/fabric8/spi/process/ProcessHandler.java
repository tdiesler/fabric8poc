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
package io.fabric8.spi.process;

import io.fabric8.api.process.ProcessOptions;

import java.util.concurrent.Future;

import org.jboss.gravia.runtime.LifecycleException;


/**
 * The process handler
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
public interface ProcessHandler {

    ManagedProcess create(ProcessOptions options, ProcessIdentity identity);

    Future<ManagedProcess> start() throws LifecycleException;

    Future<ManagedProcess> stop() throws LifecycleException;

    ManagedProcess destroy();
}
