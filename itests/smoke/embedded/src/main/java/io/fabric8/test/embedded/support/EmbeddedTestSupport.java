/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Embedded
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
package io.fabric8.test.embedded.support;

import io.fabric8.spi.BootstrapComplete;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;

/**
 * Test fabric-core servies
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Oct-2013
 */
public abstract class EmbeddedTestSupport {

    private static String[] moduleNames = new String[] { "gravia-provision", "gravia-resolver", "gravia-repository",
            "fabric8-api", "fabric8-spi", "fabric8-domain-agent", "fabric8-core",
            "fabric8-container-karaf-managed", "fabric8-container-tomcat-managed", "fabric8-container-wildfly-managed" };

    public static void beforeClass() throws Exception {

        URL location = EmbeddedTestSupport.class.getProtectionDomain().getCodeSource().getLocation();
        File basedir = new File(location.getPath()).getParentFile().getParentFile();

        System.setProperty("log4j.configuration", "file://"+new File(basedir, "src/test/resources/logging.properties").getCanonicalPath());
        System.setProperty("basedir", basedir.getCanonicalPath());
        System.setProperty("runtime.id", "embedded");
        System.setProperty("runtime.home", new File(basedir, "target/home").getCanonicalPath());
        System.setProperty("runtime.data", new File(basedir, "target/home/data").getCanonicalPath());
        System.setProperty("runtime.conf", new File(basedir, "target/home/conf").getCanonicalPath());

        // Install and start the bootstrap modules
        for (String name : moduleNames) {
            ClassLoader classLoader = EmbeddedTestSupport.class.getClassLoader();
            EmbeddedUtils.installAndStartModule(classLoader, name);
        }

        // Wait for the {@link BootstrapComplete} service
        ServiceLocator.awaitService(BootstrapComplete.class, 20, TimeUnit.SECONDS);
    }

    public static void afterClass() throws Exception {
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Assert.assertTrue(runtime.shutdown().awaitShutdown(20, TimeUnit.SECONDS));
        RuntimeLocator.releaseRuntime();
    }
}
