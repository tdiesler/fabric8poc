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

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_REMOTE_AGENT_URL;
import static io.fabric8.spi.RuntimeService.PROPERTY_REMOTE_AGENT_TYPE;
import static io.fabric8.spi.RuntimeService.PROPERTY_REMOTE_AGENT_URL;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.domain.agent.AgentLogger;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.MutableManagedProcess;
import io.fabric8.spi.process.ProcessHandler;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.spi.RuntimePropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.ObjectNameFactory;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.json.simple.JSONArray;
import org.osgi.jmx.framework.FrameworkMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Karaf {@link ProcessHandler}
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public final class KarafProcessHandler extends AbstractProcessHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(KarafProcessHandler.class);

    private Process javaProcess;

    public KarafProcessHandler(MBeanServer mbeanServer, AgentRegistration localAgent) {
        super(mbeanServer, localAgent, new RuntimePropertiesProvider());
    }

    @Override
    protected Process getJavaProcess() {
        return javaProcess;
    }

    @Override
    protected void doConfigure(MutableManagedProcess process) throws Exception {
        File karafHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(karafHome.isDirectory(), "Karaf home does not exist: " + karafHome);
        File confDir = new File(karafHome, "etc");
        IllegalStateAssertion.assertTrue(confDir.isDirectory(), "Karaf conf does not exist: " + confDir);

        String comment = "Modified by " + getClass().getName();
        configureHttpService(process, confDir, comment);
        configureKarafManagement(process, confDir, comment);
        configureZookeeper(process, confDir, comment);
    }

    protected void configureHttpService(MutableManagedProcess process, File confDir, String comment) throws IOException {

        // etc/org.apache.felix.http.cfg
        File paxwebFile = new File(confDir, "org.apache.felix.http.cfg");
        if (paxwebFile.exists()) {
            Properties props = new Properties();
            props.load(new FileReader(paxwebFile));

            KarafProcessOptions createOptions = (KarafProcessOptions) process.getCreateOptions();
            int httpPort = nextAvailablePort(createOptions.getHttpPort());
            int httpsPort = nextAvailablePort(createOptions.getHttpsPort());

            props.setProperty("org.osgi.service.http.port", "" + httpPort);
            props.setProperty("org.osgi.service.https.port", "" + httpsPort);
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

        props.setProperty("rmiRegistryPort", "" + rmiRegistryPort);
        props.setProperty("rmiServerPort", "" + rmiServerPort);
        FileWriter fileWriter = new FileWriter(managementFile);
        try {
            props.store(fileWriter, comment);
        } finally {
            fileWriter.close();
        }
    }

    protected void configureZookeeper(MutableManagedProcess process, File confDir, String comment) throws IOException {

        // etc/io.fabric8.zookeeper.server-0000.cfg
        File managementFile = new File(confDir, "io.fabric8.zookeeper.server-0000.cfg");
        IllegalStateAssertion.assertTrue(managementFile.exists(), "File does not exist: " + managementFile);
        IllegalStateAssertion.assertTrue(managementFile.delete(), "Cannot delete: " + managementFile);
    }

    @Override
    protected void doStart(MutableManagedProcess process) throws Exception {

        Path karafHome = process.getHomePath();
        Path karafData = karafHome.resolve("data");
        Path karafEtc = karafHome.resolve("etc");
        Path karafLib = karafHome.resolve("lib");
        Path karafInstances = karafHome.resolve("instances");
        IllegalStateAssertion.assertTrue(karafHome.toFile().isDirectory(), "Not a valid home dir: " + karafHome);

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        // JavaVM args
        KarafProcessOptions createOptions = (KarafProcessOptions) process.getCreateOptions();
        String javaArgs = createOptions.getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        // Karaf properties
        cmd.add("-Druntime.id=" + process.getIdentity().getName()); // Why is the runtime.id explicitly needed?
        cmd.add("-Dkaraf.name=" + process.getIdentity().getName());
        cmd.add("-Dkaraf.home=" + karafHome);
        cmd.add("-Dkaraf.base=" + karafHome);
        cmd.add("-Dkaraf.data=" + karafData);
        cmd.add("-Dkaraf.etc=" + karafEtc);
        cmd.add("-Dkaraf.instances=" + karafInstances);
        cmd.add("-Dkaraf.startLocalConsole=false");
        cmd.add("-Dkaraf.startRemoteShell=false");
        cmd.add("-D" + PROPERTY_REMOTE_AGENT_URL + "=" + process.getAttribute(ATTRIBUTE_KEY_REMOTE_AGENT_URL));
        cmd.add("-D" + PROPERTY_REMOTE_AGENT_TYPE + "=" + RuntimeType.getRuntimeType());

        // Java properties
        cmd.add("-Djava.io.tmpdir=" + karafData.resolve("tmp"));
        cmd.add("-Djava.util.logging.config.file=" + karafEtc.resolve("java.util.logging.properties"));
        cmd.add("-Djava.endorsed.dirs=" + karafLib.resolve("endorsed"));

        // Classpath
        StringBuffer classPath = new StringBuffer();
        File karafLibDir = karafLib.toFile();
        String[] libs = karafLibDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("karaf");
            }
        });
        for (String lib : libs) {
            String separator = classPath.length() > 0 ? File.pathSeparator : "";
            classPath.append(separator + karafLib.resolve(lib));
        }
        cmd.add("-classpath");
        cmd.add(classPath.toString());

        // Main class
        cmd.add("org.apache.karaf.main.Main");

        AgentLogger.LOGGER.info("Staring process with: {}", cmd);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(karafHome.toFile());
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder, createOptions);
    }

    private void startProcess(ProcessBuilder processBuilder, ProcessOptions options) throws IOException {
        javaProcess = processBuilder.start();
        new Thread(new ConsoleConsumer(javaProcess, options)).start();
    }

    @Override
    protected void doStop(MutableManagedProcess process) throws Exception {
        // [TODO] #55 Topology should manage ProcessRegistration instead of just ProcessIdentity
        // Every managed process also creates an agent with the same id as the process
        // Obtain the jolokia endpoint URL from the ProcessRegistration
        AgentIdentity agentId = AgentIdentity.create(process.getIdentity().getName());
        AgentTopology topology = ManagementUtils.getMXBeanProxy(getMBeanServer(), AgentTopology.OBJECT_NAME, AgentTopology.class);
        AgentRegistration remoteAgent = topology.getAgentRegistration(agentId);
        String serviceURL = remoteAgent.getJolokiaEndpoint();
        J4pClient client = new J4pClient(serviceURL);
        ObjectName oname = getFrameworkMBeanName(client);
        J4pExecRequest execReq = new J4pExecRequest(oname, "shutdownFramework");
        client.execute(execReq);

        // Wait for the java process to terminate
        if (javaProcess != null) {
            javaProcess.waitFor();
        }
    }

    protected ObjectName getFrameworkMBeanName(J4pClient client) throws Exception {
        J4pSearchRequest searchReq = new J4pSearchRequest(FrameworkMBean.OBJECTNAME + ",*");
        J4pResponse<J4pSearchRequest> searchRes = client.execute(searchReq);
        Object firstItem = ((JSONArray) searchRes.getValue()).get(0);
        return ObjectNameFactory.create((String) firstItem);
    }
}
