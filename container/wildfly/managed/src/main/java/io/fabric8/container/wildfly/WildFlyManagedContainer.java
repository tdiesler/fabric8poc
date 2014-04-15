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
package io.fabric8.container.wildfly;

import io.fabric8.api.Constants;
import io.fabric8.spi.AbstractManagedContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class WildFlyManagedContainer extends AbstractManagedContainer<WildFlyCreateOptions> {

    WildFlyManagedContainer(WildFlyCreateOptions options) {
        super(options);
    }

    @Override
    protected void doConfigure() throws Exception {
        String jmxServerURL = "service:jmx:http-remoting-jmx://127.0.0.1:9990";
        putAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerURL);
    }

    @Override
    protected void doStart() throws Exception {

        File jbossHome = getContainerHome();
        if (!jbossHome.isDirectory())
            throw new IllegalStateException("Not a valid WildFly home dir: " + jbossHome);

        File modulesPath = new File(jbossHome, "modules");
        File modulesJar = new File(jbossHome, "jboss-modules.jar");
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        String javaArgs = getCreateOptions().getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesPath.getAbsolutePath());
        cmd.add("org.jboss.as.standalone");
        cmd.add("-server-config");
        cmd.add(getCreateOptions().getServerConfig());

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder);
    }
}
