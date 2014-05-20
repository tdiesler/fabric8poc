/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.fabric8.test.smoke;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Dictionary;
import java.util.Hashtable;

import org.jboss.gravia.Constants;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.utils.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test URLStreamHandler integration
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-May-2014
 */
public class URLStreamHandlerTestBase {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testURLHandlerService() throws Exception {

        try {
            new URL("foo://hi");
            Assert.fail("No handler registered. Should throw a MalformedURLException.");
        } catch (MalformedURLException mue) {
            // expected
        }

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.URL_HANDLER_PROTOCOL, "foo");
        ServiceRegistration<URLStreamHandler> sreg = syscontext.registerService(URLStreamHandler.class, new MyHandler(), props);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyStream(new URL("foo://somehost").openStream(), baos);
            Assert.assertEquals("somehost", new String(baos.toByteArray()));

            URL url = new URL("foo:///somepath");
            Assert.assertEquals("", url.getHost());
        } finally {
            sreg.unregister();
        }

        try {
            new URL("foo://hi").openConnection();
            Assert.fail("No handler registered any more.");
        } catch (IOException ex) {
            // expected
        }
    }

    private static class MyHandler extends URLStreamHandler {

        @Override
        public URLConnection openConnection(final URL url) throws IOException {
            return new URLConnection(url) {

                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(url.getHost().getBytes());
                }
            };
        }
    }
}
