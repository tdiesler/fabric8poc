package io.fabric8.container.tomcat;
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



import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JOLOKIA_AGENT_PASSWORD;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JOLOKIA_AGENT_URL;
import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JOLOKIA_AGENT_USERNAME;
import static io.fabric8.spi.RuntimeService.PROPERTY_JOLOKIA_AGENT_PASSWORD;
import static io.fabric8.spi.RuntimeService.PROPERTY_JOLOKIA_AGENT_URL;
import static io.fabric8.spi.RuntimeService.PROPERTY_JOLOKIA_AGENT_USERNAME;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.MutableManagedProcess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
public final class TomcatProcessHandler extends AbstractProcessHandler {

    public TomcatProcessHandler(MBeanServer mbeanServer, AgentRegistration localAgent) {
        super(mbeanServer, localAgent, new RuntimePropertiesProvider());
    }

    @Override
    protected void doConfigure(MutableManagedProcess process) throws Exception {

        File catalinaHome = process.getHomePath().toFile();
        IllegalStateAssertion.assertTrue(catalinaHome.isDirectory(), "Catalina home does not exist: " + catalinaHome);
        File catalinaConf = new File(catalinaHome , "conf");
        IllegalStateAssertion.assertTrue(catalinaConf.isDirectory(), "Catalina conf does not exist: " + catalinaConf);
        File graviaConf = new File(catalinaConf , "gravia" + File.separator + "configs");
        IllegalStateAssertion.assertTrue(catalinaConf.isDirectory(), "Gravia conf does not exist: " + graviaConf);

        configureServer(process, catalinaConf);
    }

    protected void configureServer(MutableManagedProcess process, File confDir) throws Exception {
        // Transform conf/server.xml
        transformServerXML(process, new File(confDir, "server.xml"));
        // Transform conf/catalina.properties
        transformCatalinaProperties(process, new File(confDir, "catalina.properties"));
    }

    @Override
    protected void doStart(MutableManagedProcess process) throws Exception {

        TomcatProcessOptions createOptions = (TomcatProcessOptions) process.getCreateOptions();
        int jmxPort = nextAvailablePort(createOptions.getJmxPort());
        int ajpPort = nextAvailablePort(createOptions.getAjpPort());
        int httpPort = nextAvailablePort(createOptions.getHttpPort());
        int httpsPort = nextAvailablePort(createOptions.getHttpsPort());

        // Construct a command to execute
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
        cmd.add("-Dcom.sun.management.jmxremote.ssl=false");
        cmd.add("-Dcom.sun.management.jmxremote.authenticate=false");

        cmd.add("-Dtomcat.ajp.port=" + ajpPort);
        cmd.add("-Dtomcat.http.port=" + httpPort);
        cmd.add("-Dtomcat.https.port=" + httpsPort);
        cmd.add("-D" + PROPERTY_JOLOKIA_AGENT_URL + "=" + process.getAttribute(ATTRIBUTE_KEY_JOLOKIA_AGENT_URL));
        // [TODO] Remove JMX credentials from logged system properties
        cmd.add("-D" + PROPERTY_JOLOKIA_AGENT_USERNAME + "=" + process.getAttribute(ATTRIBUTE_KEY_JOLOKIA_AGENT_USERNAME));
        cmd.add("-D" + PROPERTY_JOLOKIA_AGENT_PASSWORD + "=" + process.getAttribute(ATTRIBUTE_KEY_JOLOKIA_AGENT_PASSWORD));

        String javaArgs = createOptions.getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        File catalinaHome = process.getHomePath().toFile();
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
        startProcess(processBuilder, createOptions);
    }

    private void transformServerXML(MutableManagedProcess process, File configFile) throws Exception {
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

    private void transformCatalinaProperties(MutableManagedProcess process, File configFile) throws Exception {
        IllegalStateAssertion.assertTrue(configFile.exists(), "File does not exist: " + configFile);

        FileInputStream instream = new FileInputStream(configFile);
        Properties properties = new Properties();
        properties.load(instream);
        IOUtils.safeClose(instream);

        FileOutputStream outstream = new FileOutputStream(configFile);
        properties.setProperty("runtime.id", process.getIdentity().getName());
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
