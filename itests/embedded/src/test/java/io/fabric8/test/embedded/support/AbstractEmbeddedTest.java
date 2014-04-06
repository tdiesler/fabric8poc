/*
 * #%L
 * Gravia :: Integration Tests :: OSGi
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package io.fabric8.test.embedded.support;

import io.fabric8.core.api.ServiceLocator;
import io.fabric8.core.service.BootstrapComplete;

import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Test fabric-core servies
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Oct-2013
 */
public abstract class AbstractEmbeddedTest {

    private static String[] moduleNames = new String[] { "fabric8-core-api", "fabric8-core-spi", "fabric8-core-service" };

    @BeforeClass
    public static void beforeClass() throws Exception {

        // Install and start the bootstrap modules
        for (String name : moduleNames) {
            ClassLoader classLoader = AbstractEmbeddedTest.class.getClassLoader();
            EmbeddedUtils.installAndStartModule(classLoader, name);
        }

        // Wait for the {@link BootstrapComplete} service
        ServiceLocator.awaitService(BootstrapComplete.class, 20, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        // Wait for the system to stabilize after possible reconfiguration
        ServiceLocator.awaitService(BootstrapComplete.class, 20, TimeUnit.SECONDS);

        RuntimeLocator.releaseRuntime();
    }
}
