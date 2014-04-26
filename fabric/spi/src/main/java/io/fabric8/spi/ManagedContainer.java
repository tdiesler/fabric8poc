/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.spi;

import io.fabric8.api.Attributable;
import io.fabric8.api.Container.State;
import io.fabric8.api.LifecycleException;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;


/**
 * The managed root container
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public interface ManagedContainer<C extends ManagedCreateOptions> extends Attributable {

    File getContainerHome();

    State getState();

    C getCreateOptions();

    String getIdentity();

    void create() throws LifecycleException;

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    void destroy() throws LifecycleException;

    JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit);
}
