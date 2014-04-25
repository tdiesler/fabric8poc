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
package io.fabric8.container.tomcat.webapp;

import io.fabric8.container.tomcat.webapp.FabricTomcatActivator.BoostrapLatch;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Wait until fabric bootstrap is complete.
 */
public class FabricBootstrapCompleteListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        BoostrapLatch latch = (BoostrapLatch) servletContext.getAttribute(BoostrapLatch.class.getName());
        try {
            // Wait for the {@link BootstrapComplete} to come up
            try {
                if (!latch.await(60, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Cannot obtain BootstrapComplete");
                }
            } catch (InterruptedException ex) {
                // ignore
            }
        } finally {
            servletContext.removeAttribute(BoostrapLatch.class.getName());
        }

        // Print banner message
        printFabricBanner(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    private void printFabricBanner(ServletContext servletContext) {

        Properties brandingProperties = new Properties();
        String resname = "/WEB-INF/branding.properties";
        try {
            URL brandingURL = servletContext.getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read branding properties from: " + resname);
        }
        String welcome = brandingProperties.getProperty("welcome");

        System.out.println(welcome);
    }
}
