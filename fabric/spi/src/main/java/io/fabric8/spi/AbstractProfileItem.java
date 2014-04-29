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
package io.fabric8.spi;

import io.fabric8.api.ProfileItem;

import org.jboss.gravia.utils.NotNullException;

public abstract class AbstractProfileItem<T extends ProfileItem> extends AttributeSupport implements ProfileItem {

    private final String identity;

    protected AbstractProfileItem(String identity) {
        NotNullException.assertValue(identity, "identity");
        this.identity = identity;
    }

    public abstract T copyProfileItem(T item);

    @Override
    public String getIdentity() {
        return identity;
    }
}
