/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.test.basic.tomcat;

import java.io.FileInputStream;

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

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test port replacement in conf/server.xml.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2014
 */
public class PortReplacementTest {

    @Test
    public void testPortReplacement() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document document = builder.parse(new FileInputStream("src/test/resources/server.xml"));

        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8080']", "${tomcat.http.port}");
        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8443']", "${tomcat.https.port}");
        replacePortValue(document, "/Server/Service[@name='Catalina']/Connector[@port='8009']", "${tomcat.ajp.port}");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);    }

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
