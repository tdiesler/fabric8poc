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

import io.fabric8.api.Constants;
import io.fabric8.container.wildfly.connector.WildFlyManagementUtils;
import io.fabric8.spi.AbstractManagedContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;
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

import org.jboss.gravia.runtime.RuntimeType;
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
public final class WildFlyManagedContainer extends AbstractManagedContainer<WildFlyCreateOptions> {

    public WildFlyManagedContainer(WildFlyCreateOptions options) {
        super(options);
    }

    @Override
    protected void doStart() throws Exception {

        File jbossHome = getContainerHome();
        IllegalStateAssertion.assertTrue(jbossHome.isDirectory(), "WildFly home does not exist: " + jbossHome);

        // Delete zookepper config file if this is not a server
        if (!getCreateOptions().isZooKeeperServer()) {
            File zooKeeperServerFile = new File(jbossHome, "standalone/configuration/gravia/configs/io.fabric8.zookeeper.server-0000.cfg");
            zooKeeperServerFile.delete();
        }

        // Transform conf/server.xml
        transformStandaloneXML(new File(jbossHome, "standalone/configuration/" + getCreateOptions().getServerConfig()));

        File modulesPath = new File(jbossHome, "modules");
        File modulesJar = new File(jbossHome, "jboss-modules.jar");
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);

        WildFlyCreateOptions options = getCreateOptions();
        int managementNativePort = nextAvailablePort(options.getManagementNativePort());
        int managementHttpPort = nextAvailablePort(options.getManagementHttpPort());
        int managementHttpsPort = nextAvailablePort(options.getManagementHttpsPort());
        int ajpPort = nextAvailablePort(options.getAjpPort());
        int httpPort = nextAvailablePort(options.getHttpPort());
        int httpsPort = nextAvailablePort(options.getHttpsPort());

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-Djboss.management.native.port=" + managementNativePort);
        cmd.add("-Djboss.management.http.port=" + managementHttpPort);
        cmd.add("-Djboss.management.https.port=" + managementHttpsPort);
        cmd.add("-Djboss.ajp.port=" + ajpPort);
        cmd.add("-Djboss.http.port=" + httpPort);
        cmd.add("-Djboss.https.port=" + httpsPort);

        String javaArgs = options.getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesPath.getAbsolutePath());
        cmd.add("org.jboss.as.standalone");
        cmd.add("-server-config");
        cmd.add(options.getServerConfig());

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder);

        putAttribute(Constants.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
        putAttribute(Constants.ATTRIBUTE_KEY_HTTPS_PORT, httpsPort);

        String jmxServerURL = "service:jmx:http-remoting-jmx://127.0.0.1:" + managementHttpPort;
        putAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerURL);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, long timeout, TimeUnit unit) {
        JMXConnector connector;
        if (RuntimeType.getRuntimeType() == RuntimeType.WILDFLY) {
            JmxEnvironmentEnhancer.addClassLoader(env);
            connector = super.getJMXConnector(env, timeout, unit);
        } else {
            String jmxServiceURL = getAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL);
            return WildFlyManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
        }
        return connector;
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

    private void transformStandaloneXML(File configFile) throws Exception {
        IllegalStateAssertion.assertTrue(configFile.exists(), "File does not exist: " + configFile);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        FileInputStream fis = new FileInputStream(configFile);
        Document document = builder.parse(fis);
        fis.close();

        String containerId = getCreateOptions().getIdentity().getCanonicalForm();
        replacePropertyValue(document, "/server/system-properties/property[@name='runtime.id']", containerId);

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
