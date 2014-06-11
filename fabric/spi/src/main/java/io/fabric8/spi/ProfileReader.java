/*
 * #%L
 * Gravia :: Repository
 * %%
 * Copyright (C) 2012 - 2014 JBoss by Red Hat
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

import java.io.IOException;

import org.jboss.gravia.resource.Version;

/**
 * A reader for profiles.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2014
 */
public interface ProfileReader {

    Version getProfileVersion() throws IOException;

    Profile nextProfile();

    void close();
}
