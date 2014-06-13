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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A profile identity
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class ProfileIdentity extends AbstractIdentity {

    private static final String IDENTITY_FORMAT = "(%s)(,rev=(?<rev>%s))?";
    private static final Pattern IDENTITY_PATTERN = Pattern.compile(String.format(IDENTITY_FORMAT, GROUP, GROUP));

    private final String revision;

    public static ProfileIdentity create(String symbolicName) {
        return new ProfileIdentity(symbolicName, null);
    }

    public static ProfileIdentity create(String symbolicName, String revision) {
        return new ProfileIdentity(symbolicName, revision);
    }

    public static ProfileIdentity createFrom(String canonical) {
        Matcher matcher = assertCanonicalForm(IDENTITY_PATTERN, canonical);
        String symbolicName = matcher.group(1);
        String revision = matcher.group("rev");
        return new ProfileIdentity(symbolicName, revision);
    }

    private ProfileIdentity(String symbolicName, String revision) {
        super(symbolicName);
        this.revision = revision;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public String getCanonicalForm() {
        return getSymbolicName() + (revision != null ? ",rev=" + revision : "");
    }
}
