/*
 * #%L
 * Fabric8 :: SPI
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

import io.fabric8.api.AttributeKey;



/**
 * The mutable managed process
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public interface MutableManagedProcess extends ManagedProcess {

    <V> V putAttribute(AttributeKey<V> key, V value);

    <V> V removeAttribute(AttributeKey<V> key);

    void setState(State state);
}
