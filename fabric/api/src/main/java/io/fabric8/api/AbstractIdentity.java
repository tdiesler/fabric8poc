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

import org.jboss.gravia.utils.IllegalArgumentAssertion;


/**
 * An abstract string based identity
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
abstract class AbstractIdentity implements Identity {

    private static final Pattern NAME_PATTERN = Pattern.compile(String.format("(%s)", GROUP));

    private final String symbolicName;

    AbstractIdentity(String symbolicName) {
        IllegalArgumentAssertion.assertNotNull(symbolicName, "symbolicName");
        this.symbolicName = symbolicName;
        assertCanonicalForm(NAME_PATTERN, symbolicName);
    }

    static Matcher assertCanonicalForm(Pattern pattern, String canonical) {
        IllegalArgumentAssertion.assertNotNull(canonical, "canonical");
        Matcher matcher = pattern.matcher(canonical);
        IllegalArgumentAssertion.assertTrue(matcher.matches(), "Parameter '" + canonical + "' does not match pattern: " + pattern);
        return matcher;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public String getCanonicalForm() {
        return symbolicName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractIdentity)) return false;
        AbstractIdentity other = (AbstractIdentity) obj;
        return other.getCanonicalForm().equals(getCanonicalForm());
    }

    @Override
    public int hashCode() {
        return getCanonicalForm().hashCode();
    }

    @Override
    public String toString() {
        return getCanonicalForm();
    }
}
