/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package io.fabric8.test.smoke;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.gravia.itests.support.HttpRequest;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpService;

/**
 * Test the {@link HttpService}
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2013
 */
public abstract class HttpServiceTestBase {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testHttpServiceClassLoading() throws Exception {

        Assume.assumeTrue(RuntimeType.getRuntimeType() == RuntimeType.WILDFLY);

        // Get the org.jboss.gravia class loader
        ClassLoader classLoader = RuntimeType.class.getClassLoader();
        Assert.assertTrue("Unexpected: " + classLoader, classLoader.toString().contains("org.jboss.gravia"));

        // Load the HttpService through module org.jboss.gravia
        Class<?> serviceClass = classLoader.loadClass(HttpService.class.getName());
        String loaderName = serviceClass.getClassLoader().toString();

        // Assert that the loaded class comes from org.osgi.enterprise
        Assert.assertTrue("Unexpected: " + loaderName, loaderName.contains("org.osgi.enterprise"));
    }

    @Test
    public void testServletAccess() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module module = runtime.getModule(getClass().getClassLoader());
        HttpService httpService = ServiceLocator.getRequiredService(HttpService.class);

        // Verify that the alias is not yet available
        String reqspec = "/fabric8/service?test=param&param=Kermit";
        assertNotAvailable(reqspec);

        // Register the test servlet and make a call
        httpService.registerServlet("/service", new HttpServiceServlet(module), null, null);
        Assert.assertEquals("Hello: Kermit", performCall(reqspec));

        // Unregister the servlet alias
        httpService.unregister("/service");
        assertNotAvailable(reqspec);

        // Verify that the alias is not available any more
        assertNotAvailable(reqspec);
    }

    private void assertNotAvailable(String reqspec) throws Exception {
        try {
            performCall(reqspec, null, 500, TimeUnit.MILLISECONDS);
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }
    }

    private String performCall(String path) throws Exception {
        return performCall(path, null, 2, TimeUnit.SECONDS);
    }

    private String performCall(String path, Map<String, String> headers, long timeout, TimeUnit unit) throws Exception {
        Object port = RuntimeLocator.getRequiredRuntime().getProperty("org.osgi.service.http.port", "8080");
        return HttpRequest.get("http://127.0.0.1:" + port + path, headers, timeout, unit);
    }

    @SuppressWarnings("serial")
    static final class HttpServiceServlet extends HttpServlet {

        private final Module module;

        // This hides the default ctor and verifies that this instance is used
        HttpServiceServlet(Module module) {
            this.module = module;
        }

        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            PrintWriter out = res.getWriter();
            String type = req.getParameter("test");
            if ("param".equals(type)) {
                String value = req.getParameter("param");
                out.print("Hello: " + value);
            } else if ("init".equals(type)) {
                String key = req.getParameter("init");
                String value = getInitParameter(key);
                out.print(key + "=" + value);
            } else if ("module".equals(type)) {
                out.print(module.getIdentity());
            } else {
                throw new IllegalArgumentException("Invalid 'test' parameter: " + type);
            }
            out.close();
        }
    }
}
