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
package io.fabric8.test.smoke.suba;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.gravia.runtime.ModuleActivator;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceTracker;
import org.osgi.service.http.HttpService;

public class SimpleEndpointActivator implements ModuleActivator {

    private ServiceTracker<HttpService, HttpService> tracker;
    private HttpService httpService;

    @Override
    public void start(final ModuleContext context) throws Exception {

        // [FELIX-4415] Cannot associate HttpService instance with ServletContext
        tracker = new ServiceTracker<HttpService, HttpService>(context, HttpService.class, null) {
            @Override
            public HttpService addingService(ServiceReference<HttpService> sref) {
                if (httpService == null) {
                    httpService = super.addingService(sref);
                    registerHttpServiceServlet(httpService);
                }
                return httpService;
            }
        };
        tracker.open();
    }

    @Override
    public void stop(ModuleContext context) throws Exception {
        if (tracker != null) {
            tracker.close();
        }
        if (httpService != null) {
            httpService.unregister("/service");
        }
    }

    private void registerHttpServiceServlet(HttpService httpService) {
        try {
            httpService.registerServlet("/service", new HttpServiceServlet(), null, null);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot register HttpServiceServlet", ex);
        }
    }

    @SuppressWarnings("serial")
    static final class HttpServiceServlet extends HttpServlet {

        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            PrintWriter out = res.getWriter();
            String msg = req.getParameter("test");
            out.print(msg);
            out.close();
        }
    }
}
