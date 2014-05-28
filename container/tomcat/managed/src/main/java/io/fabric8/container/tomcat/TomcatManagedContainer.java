/*
 * #%L
 * Fabric8 :: Container :: Tomcat :: Managed
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

package io.fabric8.container.tomcat;

import io.fabric8.api.Constants;
import io.fabric8.spi.AbstractManagedContainer;
import io.fabric8.spi.RuntimeService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class TomcatManagedContainer extends AbstractManagedContainer<TomcatCreateOptions> {

    TomcatManagedContainer(TomcatCreateOptions options) {
        super(options);
    }

    @Override
    protected void doConfigure() throws Exception {

        File catalinaHome = getContainerHome();
        IllegalStateAssertion.assertTrue(catalinaHome.isDirectory(), "Catalina home does not exist: " + catalinaHome);

        // Delete zookepper config file if this is not a server
        if (!getCreateOptions().isZooKeeperServer()) {
            File zooKeeperServerFile = new File(catalinaHome, "conf/gravia/configs/io.fabric8.zookeeper.server-0000.cfg");
            zooKeeperServerFile.delete();
        }

        // Transform conf/server.xml
        transformServerXML(new File(catalinaHome, "conf/server.xml"));

        // Transform conf/catalina.properties
        transformCatalinaProperties(new File(catalinaHome, "conf/catalina.properties"));
    }

    @Override
    protected void doStart() throws Exception {

        int jmxPort = nextAvailablePort(getCreateOptions().getJmxPort());
        int ajpPort = nextAvailablePort(getCreateOptions().getAjpPort());
        int httpPort = nextAvailablePort(getCreateOptions().getHttpPort());
        int httpsPort = nextAvailablePort(getCreateOptions().getHttpsPort());

        // Construct a command to execute
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
        cmd.add("-Dcom.sun.management.jmxremote.ssl=false");
        cmd.add("-Dcom.sun.management.jmxremote.authenticate=false");

        cmd.add("-Dtomcat.ajp.port=" + ajpPort);
        cmd.add("-Dtomcat.http.port=" + httpPort);
        cmd.add("-Dtomcat.https.port=" + httpsPort);

        String javaArgs = getCreateOptions().getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        File catalinaHome = getContainerHome();
        String absolutePath = catalinaHome.getAbsolutePath();
        String CLASS_PATH = absolutePath + "/bin/bootstrap.jar" + File.pathSeparator;
        CLASS_PATH += absolutePath + "/bin/tomcat-juli.jar";

        cmd.add("-classpath");
        cmd.add(CLASS_PATH);
        cmd.add("-Djava.endorsed.dirs=" + absolutePath + "/endorsed");
        cmd.add("-Dcatalina.base=" + absolutePath);
        cmd.add("-Dcatalina.home=" + absolutePath);
        cmd.add("-Djava.io.tmpdir=" + absolutePath + "/temp");
        cmd.add("org.apache.catalina.startup.Bootstrap");
        cmd.add("start");

        // execute command
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(catalinaHome, "bin"));
        startProcess(processBuilder);

        putAttribute(Constants.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
        putAttribute(Constants.ATTRIBUTE_KEY_HTTPS_PORT, httpsPort);

        String jmxServerURL = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi";
        putAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerURL);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, long timeout, TimeUnit unit) {
        JMXConnector connector;
        if (RuntimeType.getRuntimeType() == RuntimeType.WILDFLY) {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(null);
                connector = super.getJMXConnector(env, timeout, unit);
            } finally {
                Thread.currentThread().setContextClassLoader(contextLoader);
            }
        } else {
            connector = super.getJMXConnector(env, timeout, unit);
        }
        return connector;
    }

    private void transformServerXML(File configFile) throws Exception {
        IllegalStateAssertion.assertTrue(configFile.exists(), "File does not exist: " + configFile);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        FileInputStream fis = new FileInputStream(configFile);
        Document document = builder.parse(fis);
        fis.close();

        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8080']", "${tomcat.http.port}");
        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8443']", "${tomcat.https.port}");
        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8009']", "${tomcat.ajp.port}");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        FileOutputStream fos = new FileOutputStream(configFile);
        StreamResult result = new StreamResult(fos);
        transformer.transform(source, result);
        fos.close();
    }

    private void transformCatalinaProperties(File configFile) throws Exception {
        IllegalStateAssertion.assertTrue(configFile.exists(), "File does not exist: " + configFile);

        FileInputStream instream = new FileInputStream(configFile);
        Properties properties = new Properties();
        properties.load(instream);
        IOUtils.safeClose(instream);

        FileOutputStream outstream = new FileOutputStream(configFile);
        properties.setProperty(RuntimeService.RUNTIME_IDENTITY, getIdentity().getCanonicalForm());
        properties.store(outstream, "Managed container properties");
        IOUtils.safeClose(outstream);
    }

    private void replacePortValue(Document document, String expression, String replacement) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element element = (Element) xPath.compile(expression).evaluate(document, XPathConstants.NODE);
        if (element != null) {
            element.setAttribute("port", replacement);
            Attr attrNode = element.getAttributeNode("redirectPort");
            if (attrNode != null) {
                element.setAttribute("redirectPort", "${tomcat.https.port}");
            }
        }
    }
}
