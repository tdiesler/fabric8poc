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
package io.fabric8.test.embedded.support;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.spi.AbstractContainerBuilder;
import io.fabric8.spi.AbstractCreateOptions;
import io.fabric8.test.embedded.support.EmbeddedContainerBuilder.EmbeddedCreateOptions;
import org.jboss.gravia.runtime.RuntimeType;

public final class EmbeddedContainerBuilder extends AbstractContainerBuilder<EmbeddedContainerBuilder, EmbeddedCreateOptions> {

    public static EmbeddedContainerBuilder create() {
        return new EmbeddedContainerBuilder();
    }
    public static EmbeddedContainerBuilder create(String identity) {
        return new EmbeddedContainerBuilder(identity);
    }

    private EmbeddedContainerBuilder() {
        super(new EmbeddedCreateOptions());
    }

    private EmbeddedContainerBuilder(String identity) {
        super(new EmbeddedCreateOptions(identity));
    }

    static class EmbeddedCreateOptions extends AbstractCreateOptions {

        EmbeddedCreateOptions() {
        }

        EmbeddedCreateOptions(String identity) {
            setIdentity(ContainerIdentity.createFrom(identity));
        }

        @Override
        public RuntimeType getRuntimeType() {
            return RuntimeType.OTHER;
        }
    }
}
