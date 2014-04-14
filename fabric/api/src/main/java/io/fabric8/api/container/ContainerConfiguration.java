/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.repository.MavenCoordinates;


/**
 * The managed container configuration
 *
 * @since 26-Feb-2014
 */
public abstract class ContainerConfiguration {

    private List<MavenCoordinates> mavenCoordinates = new ArrayList<MavenCoordinates>();
    private boolean outputToConsole;
    private String javaVmArguments;
    private File targetDirectory;
    private int portOffset;

    private boolean immutable;

    /**
     * Get the array of maven artefacts that are getting unpacked
     * during {@link ManagedContainer#create(ContainerConfiguration)}
     */
    public List<MavenCoordinates> getMavenCoordinates() {
        return Collections.unmodifiableList(mavenCoordinates);
    }

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public String getJavaVmArguments() {
        return javaVmArguments;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    /*
     * Setters are package protected
     */

    void addMavenCoordinates(MavenCoordinates coordinates) {
        assertMutable();
        mavenCoordinates.add(coordinates);
    }

    void setTargetDirectory(File target) {
        assertMutable();
        this.targetDirectory = target;
    }

    void setJavaVmArguments(String javaVmArguments) {
        assertMutable();
        this.javaVmArguments = javaVmArguments;
    }

    void setPortOffset(int portOffset) {
        this.portOffset = portOffset;
    }

    void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    void makeImmutable() {
        assertMutable();
        immutable = true;
    }

    protected void assertMutable() {
        if (immutable)
            throw new IllegalStateException("Configuration is immutable");
    }
}