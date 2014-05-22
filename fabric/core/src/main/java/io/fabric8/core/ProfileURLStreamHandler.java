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

package io.fabric8.core;

import io.fabric8.spi.PermitManagerLocator;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * The URLStreamHandler for protocol profile://
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
final class ProfileURLStreamHandler extends URLStreamHandler {

    static final String PROTOCOL_NAME = "profile";

    private final File targetFile;

    ProfileURLStreamHandler() {
        this.targetFile = null;
    }

    ProfileURLStreamHandler(File targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (targetFile != null) {
            return targetFile.toURI().toURL().openConnection();
        } else {
            PermitManager permitManager = PermitManagerLocator.getPermitManager();
            Permit<ProfileService> permit = permitManager.aquirePermit(ProfileService.PERMIT, false);
            try {
                ProfileService service = permit.getInstance();
                return service.getProfileURLConnection(url);
            } finally {
                permit.release();
            }
        }
    }
}
