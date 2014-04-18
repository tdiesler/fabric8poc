/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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

import io.fabric8.spi.utils.IllegalStateAssertion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.repository.MavenCoordinates;


public abstract class AbstractManagedCreateOptions extends AbstractCreateOptions implements ManagedCreateOptions {

    private List<MavenCoordinates> mavenCoordinates = new ArrayList<MavenCoordinates>();
    private boolean outputToConsole;
    private String javaVmArguments;
    private File targetDirectory;

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

    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    /*
     * Setters are protected
     */

    protected void addMavenCoordinates(MavenCoordinates coordinates) {
        assertMutable();
        mavenCoordinates.add(coordinates);
    }

    protected void setTargetDirectory(File target) {
        assertMutable();
        this.targetDirectory = target;
    }

    protected void setJavaVmArguments(String javaVmArguments) {
        assertMutable();
        this.javaVmArguments = javaVmArguments;
    }

    protected void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    @Override
    protected void validateConfiguration() {
        IllegalStateAssertion.assertNotNull(targetDirectory, "targetDirectory");
        super.validateConfiguration();
    }
}
