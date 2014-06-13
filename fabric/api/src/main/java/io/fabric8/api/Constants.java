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


/**
 * Fabric constants
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface Constants {

    /**
     * The default profile version
     */
    VersionIdentity DEFAULT_PROFILE_VERSION = VersionIdentity.createFrom("1.0");

    /**
     * The default profile name
     */
    ProfileIdentity DEFAULT_PROFILE_IDENTITY = ProfileIdentity.createFrom("default");

    /**
     * The management domain
     */
    String MANAGEMENT_DOMAIN = "fabric8";


}
