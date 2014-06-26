/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Managed
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

package io.fabric8.container.wildfly;

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_REMOTE_AGENT_URL;
import static io.fabric8.spi.RuntimeService.PROPERTY_REMOTE_AGENT_URL;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.MutableManagedProcess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jboss.gravia.runtime.spi.RuntimePropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class WildFlyProcessHandler extends AbstractProcessHandler {

    private Process javaProcess;

    public WildFlyProcessHandler(MBeanServer mbeanServer, AgentRegistration localAgent) {
        super(mbeanServer, localAgent, new RuntimePropertiesProvider());
    }

    @Override
    protected Process getJavaProcess() {
        return javaProcess;
    }

    @Override
    protected void doConfigure(MutableManagedProcess process) throws Exception {
        Path jbossHome = process.getHomePath();
        Path graviaConf = jbossHome.resolve(Paths.get("standalone", "configuration", "gravia", "configs"));

        IllegalStateAssertion.assertTrue(jbossHome.toFile().isDirectory(), "Wildfly home does not exist: " + jbossHome);
        IllegalStateAssertion.assertTrue(graviaConf.toFile().isDirectory(), "Gravia conf does not exist: " + jbossHome);

        configureZookeeper(process, graviaConf.toFile());
    }

    protected void configureZookeeper(MutableManagedProcess process, File confDir) throws IOException {
        // etc/io.fabric8.zookeeper.server-0000.cfg
        File managementFile = new File(confDir, "io.fabric8.zookeeper.server-0000.cfg");
        IllegalStateAssertion.assertTrue(managementFile.exists(), "File does not exist: " + managementFile);
        IllegalStateAssertion.assertTrue(managementFile.delete(), "Cannot delete: " + managementFile);
    }

    @Override
    protected void doStart(MutableManagedProcess process) throws Exception {

        File jbossHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(jbossHome.isDirectory(), "WildFly home does not exist: " + jbossHome);

        // Transform conf/server.xml
        WildFlyProcessOptions createOptions = (WildFlyProcessOptions) process.getCreateOptions();
        transformStandaloneXML(process, new File(jbossHome, "standalone/configuration/" + createOptions.getServerConfig()));

        File modulesPath = new File(jbossHome, "modules");
        File modulesJar = new File(jbossHome, "jboss-modules.jar");
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);

        int managementNativePort = nextAvailablePort(createOptions.getManagementNativePort());
        int managementHttpPort = nextAvailablePort(createOptions.getManagementHttpPort());
        int managementHttpsPort = nextAvailablePort(createOptions.getManagementHttpsPort());
        int ajpPort = nextAvailablePort(createOptions.getAjpPort());
        int httpPort = nextAvailablePort(createOptions.getHttpPort());
        int httpsPort = nextAvailablePort(createOptions.getHttpsPort());

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-Djboss.management.native.port=" + managementNativePort);
        cmd.add("-Djboss.management.http.port=" + managementHttpPort);
        cmd.add("-Djboss.management.https.port=" + managementHttpsPort);
        cmd.add("-Djboss.ajp.port=" + ajpPort);
        cmd.add("-Djboss.http.port=" + httpPort);
        cmd.add("-Djboss.https.port=" + httpsPort);
        cmd.add("-D" + PROPERTY_REMOTE_AGENT_URL + "=" + process.getAttribute(ATTRIBUTE_KEY_REMOTE_AGENT_URL));

        String javaArgs = createOptions.getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesPath.getAbsolutePath());
        cmd.add("org.jboss.as.standalone");
        cmd.add("-server-config");
        cmd.add(createOptions.getServerConfig());

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder, createOptions);
    }

    @Override
    protected void doStop(MutableManagedProcess process) throws Exception {
        destroyProcess();
    }

    private void startProcess(ProcessBuilder processBuilder, ProcessOptions options) throws IOException {
        javaProcess = processBuilder.start();
        new Thread(new ConsoleConsumer(javaProcess, options)).start();
    }

    private void destroyProcess() throws Exception {
        if (javaProcess != null) {
            javaProcess.destroy();
            javaProcess.waitFor();
        }
    }

    private void transformStandaloneXML(MutableManagedProcess process, File configFile) throws Exception {
        IllegalStateAssertion.assertTrue(configFile.exists(), "File does not exist: " + configFile);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        FileInputStream fis = new FileInputStream(configFile);
        Document document = builder.parse(fis);
        fis.close();

        String processId = process.getIdentity().getName();
        replacePropertyValue(document, "/server/system-properties/property[@name='runtime.id']", processId);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        FileOutputStream fos = new FileOutputStream(configFile);
        StreamResult result = new StreamResult(fos);
        transformer.transform(source, result);
        fos.close();
    }

    private void replacePropertyValue(Document document, String expression, String replacement) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element element = (Element) xPath.compile(expression).evaluate(document, XPathConstants.NODE);
        if (element != null) {
            element.setAttribute("value", replacement);
        }
    }
}
