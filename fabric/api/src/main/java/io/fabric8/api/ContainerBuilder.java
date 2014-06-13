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


import java.util.List;

/**
 * A builder for a fabric container
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ContainerBuilder<B extends ContainerBuilder<B, T>, T extends CreateOptions> extends AttributableBuilder<B> {

    B identity(String identity);

    B profileVersion(VersionIdentity version);

    B profiles(List<String> profiles);

    T getCreateOptions();
}
