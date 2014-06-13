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

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.permit.PermitKey;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * The internal profile service
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileService extends ProfileManager {

    /**
     * The {@link PermitKey} that protects this service.
     */
    PermitKey<ProfileService> PERMIT = new PermitKey<ProfileService>(ProfileService.class);

    ProfileVersion getRequiredProfileVersion(VersionIdentity version);

    Profile getRequiredProfile(VersionIdentity version, ProfileIdentity identity);

    /**
     * Get an url connection to content in the profile registry
     *
     * Accepted URL are formated like
     *
     * profile://[profileVersion]/[profileName]/[symbolicName]?version=[version]&cntindex=[contentIndex]
     *
     * Both, the version and cntindex parameters are optional.
     * If they are missing it will use the higest version and the first content respectively.
     *
     * [TODO] consider content selection based on runtime type
     */
    URLConnection getProfileURLConnection(URL url) throws IOException;
}
