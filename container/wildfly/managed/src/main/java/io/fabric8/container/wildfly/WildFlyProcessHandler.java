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

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_PASSWORD;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_USERNAME;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.MutableManagedProcess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class WildFlyProcessHandler extends AbstractProcessHandler {

    public WildFlyProcessHandler() {
        super(new RuntimePropertiesProvider());
    }

    @Override
    protected void doConfigure(MutableManagedProcess process) throws Exception {
        File jbossHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(jbossHome.isDirectory(), "Wildfly home does not exist: " + jbossHome);
        File graviaConf = Paths.get(jbossHome.getPath(), "standalone", "configuration", "gravia", "configs").toFile();
        IllegalStateAssertion.assertTrue(graviaConf.isDirectory(), "Gravia conf does not exist: " + jbossHome);
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
        cmd.add("-Dfabric8.agent.jmx.server.url=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL));
        // [TODO] Remove JMX credentials from logged system properties
        cmd.add("-Dfabric8.agent.jmx.username=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_USERNAME));
        cmd.add("-Dfabric8.agent.jmx.password=" + process.getAttribute(ATTRIBUTE_KEY_AGENT_JMX_PASSWORD));

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

    // Confine the usage of jboss-modules to an additional class
    // This prevents NoClassDefFoundError: org/jboss/modules/ModuleLoadException
    static class JmxEnvironmentEnhancer {
        static void addClassLoader(Map<String, Object> env) {
            String classLoaderKey = "jmx.remote.protocol.provider.class.loader";
            if (env.get(classLoaderKey) == null) {
                ClassLoader classLoader;
                try {
                    ModuleIdentifier moduleid = ModuleIdentifier.fromString("org.jboss.remoting-jmx");
                    classLoader = Module.getBootModuleLoader().loadModule(moduleid).getClassLoader();
                } catch (ModuleLoadException ex) {
                    throw new IllegalStateException(ex);
                }
                env.put(classLoaderKey, classLoader);
            }
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
