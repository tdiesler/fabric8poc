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
package io.fabric8.spi.process;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.AttributeSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.gravia.resource.MavenCoordinates;
import org.jboss.gravia.utils.IllegalStateAssertion;


public abstract class AbstractProcessOptions implements ProcessOptions {

    private final AttributeSupport attributes = new AttributeSupport();
    private final List<MavenCoordinates> mavenCoordinates = new ArrayList<MavenCoordinates>();
    private final AtomicBoolean immutable = new AtomicBoolean();
    private boolean outputToConsole;
    private String identityPrefix;
    private String javaVmArguments;
    private Path targetPath;

    @Override
    public String getIdentityPrefix() {
        return identityPrefix;
    }

    @Override
    public List<MavenCoordinates> getMavenCoordinates() {
        return Collections.unmodifiableList(mavenCoordinates);
    }

    @Override
    public Path getTargetPath() {
        return targetPath;
    }

    @Override
    public String getJavaVmArguments() {
        return javaVmArguments;
    }

    @Override
    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributes.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return attributes.getAttributes();
    }

    public void setIdentityPrefix(String identityPrefix) {
        assertMutable();
        this.identityPrefix = identityPrefix;
    }

    public void addMavenCoordinates(MavenCoordinates coordinates) {
        assertMutable();
        mavenCoordinates.add(coordinates);
    }

    public void setTargetPath(Path targetPath) {
        assertMutable();
        this.targetPath = targetPath;
    }

    public void setJavaVmArguments(String javaVmArguments) {
        assertMutable();
        this.javaVmArguments = javaVmArguments;
    }

    public void setOutputToConsole(boolean outputToConsole) {
        assertMutable();
        this.outputToConsole = outputToConsole;
    }

    public <V> void addAttribute(AttributeKey<V> key, V value) {
        assertMutable();
        attributes.putAttribute(key, value);
    }

    public void addAttributes(Map<AttributeKey<?>, Object> atts) {
        assertMutable();
        attributes.putAllAttributes(atts);
    }

    protected void validate() {
        IllegalStateAssertion.assertNotNull(identityPrefix, "identityPrefix");
        IllegalStateAssertion.assertNotNull(targetPath, "targetPath");
        immutable.set(true);
    }

    private void assertMutable() {
        IllegalStateAssertion.assertFalse(immutable.get(), "Cannot modify immutable options");
    }
}
