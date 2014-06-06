/*
 * #%L
 * Fabric8 :: Container :: Karaf :: Managed
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

package io.fabric8.container.karaf;

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_PASSWORD;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_USERNAME;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.MutableManagedProcess;
import io.fabric8.spi.process.ProcessHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jboss.gravia.runtime.spi.RuntimePropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The Karaf {@link ProcessHandler}
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public final class KarafProcessHandler extends AbstractProcessHandler {

    public KarafProcessHandler() {
        super(new RuntimePropertiesProvider());
    }

    @Override
    protected void doConfigure(MutableManagedProcess process) throws Exception {
        File karafHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(karafHome.isDirectory(), "Karaf home does not exist: " + karafHome);
        File confDir = new File(karafHome, "etc");
        IllegalStateAssertion.assertTrue(confDir.isDirectory(), "Karaf conf does not exist: " + confDir);

        String comment = "Modified by " + getClass().getName();
        configurePaxWeb(process, confDir, comment);
        configureKarafManagement(process, confDir, comment);
    }

    protected void configurePaxWeb(MutableManagedProcess process, File confDir, String comment) throws IOException {
        // etc/org.ops4j.pax.web.cfg
        File paxwebFile = new File(confDir, "org.ops4j.pax.web.cfg");
        if (paxwebFile.exists()) {
            Properties props = new Properties();
            props.load(new FileReader(paxwebFile));

            KarafProcessOptions createOptions = (KarafProcessOptions) process.getCreateOptions();
            int httpPort = nextAvailablePort(createOptions.getHttpPort());
            int httpsPort = nextAvailablePort(createOptions.getHttpsPort());

            props.setProperty("org.osgi.service.http.port", new Integer(httpPort).toString());
            props.setProperty("org.osgi.service.https.port", new Integer(httpsPort).toString());
            FileWriter fileWriter = new FileWriter(paxwebFile);
            try {
                props.store(fileWriter, comment);
            } finally {
                fileWriter.close();
            }
        }
    }

    protected void configureKarafManagement(MutableManagedProcess process, File confDir, String comment) throws IOException {
        // etc/org.apache.karaf.management.cfg
        File managementFile = new File(confDir, "org.apache.karaf.management.cfg");
        IllegalStateAssertion.assertTrue(managementFile.exists(), "File does not exist: " + managementFile);

        Properties props = new Properties();
        props.load(new FileReader(managementFile));
        KarafProcessOptions createOptions = (KarafProcessOptions) process.getCreateOptions();
        int rmiRegistryPort = nextAvailablePort(createOptions.getRmiRegistryPort());
        int rmiServerPort = nextAvailablePort(createOptions.getRmiServerPort());

        props.setProperty("rmiRegistryPort", new Integer(rmiRegistryPort).toString());
        props.setProperty("rmiServerPort", new Integer(rmiServerPort).toString());
        FileWriter fileWriter = new FileWriter(managementFile);

        try {
            props.store(fileWriter, comment);
        } finally {
            fileWriter.close();
        }
    }

    @Override
    protected void doStart(MutableManagedProcess process) throws Exception {

        File karafHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(karafHome.isDirectory(), "Not a valid home dir: " + karafHome);

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        // JavaVM args
        KarafProcessOptions createOptions = (KarafProcessOptions) process.getCreateOptions();
        String javaArgs = createOptions.getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        // Karaf properties
        cmd.add("-Druntime.id=" + process.getIdentity().getName());
        cmd.add("-Druntime.home=" + karafHome);
        cmd.add("-Druntime.base=" + karafHome);
        cmd.add("-Druntime.conf=" + karafHome + "/etc");
        cmd.add("-Druntime.data=" + karafHome + "/data");
        cmd.add("-Dkaraf.instances=" + karafHome + "/instances");
        cmd.add("-Dkaraf.startLocalConsole=false");
        cmd.add("-Dkaraf.startRemoteShell=false");
        cmd.add("-Dfabric8.agent.jmx.server.url=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL));
        // [TODO] Remove JMX credentials from logged system properties
        cmd.add("-Dfabric8.agent.jmx.username=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_USERNAME));
        cmd.add("-Dfabric8.agent.jmx.password=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_PASSWORD));

        // Java properties
        cmd.add("-Djava.io.tmpdir=" + new File(karafHome, "data/tmp"));
        cmd.add("-Djava.util.logging.config.file=" + new File(karafHome, "etc/java.util.logging.properties"));
        cmd.add("-Djava.endorsed.dirs=" + new File(karafHome, "lib/endorsed"));

        // Classpath
        StringBuffer classPath = new StringBuffer();
        File karafLibDir = new File(karafHome, "lib");
        String[] libs = karafLibDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("karaf");
            }
        });
        for (String lib : libs) {
            String separator = classPath.length() > 0 ? File.pathSeparator : "";
            classPath.append(separator + new File(karafHome, "lib/" + lib));
        }
        cmd.add("-classpath");
        cmd.add(classPath.toString());

        // Main class
        cmd.add("org.apache.karaf.main.Main");

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(karafHome);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder, createOptions);
    }
}
