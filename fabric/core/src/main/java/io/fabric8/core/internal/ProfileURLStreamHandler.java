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

package io.fabric8.core.internal;

import io.fabric8.spi.utils.IllegalStateAssertion;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jboss.gravia.utils.NotNullException;

/**
 * The URLStreamHandler for protocol profile://
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
final class ProfileURLStreamHandler extends URLStreamHandler {

    private final File targetFile;

    ProfileURLStreamHandler(File targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        NotNullException.assertValue(url, "url");
        IllegalStateAssertion.assertEquals("profile", url.getProtocol(), "Invalid protocol: " + url);
        return targetFile.toURI().toURL().openConnection();
    }

}
