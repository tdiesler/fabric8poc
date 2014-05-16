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
package io.fabric8.test.smoke.embedded;

import io.fabric8.test.embedded.support.EmbeddedTestSupport;
import io.fabric8.test.smoke.ResourceItemsTestBase;

import java.io.InputStream;

import org.jboss.gravia.arquillian.container.ContainerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@ContainerSetup(ResourceItemsTestBase.Setup.class)
public class ResourceItemsTest extends ResourceItemsTestBase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Override
    protected InputStream getDeployment(String name) {
        InputStream inputStream = null;
        if (RESOURCE_A.equals(name)) {
            Archive<?> archive = ResourceItemsTestBase.getResourceA();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        } else if (RESOURCE_B.equals(name)) {
            Archive<?> archive = ResourceItemsTestBase.getResourceB();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        } else if (RESOURCE_B1.equals(name)) {
            Archive<?> archive = ResourceItemsTestBase.getResourceB1();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        } else if (RESOURCE_C.equals(name)) {
            Archive<?> archive = ResourceItemsTestBase.getResourceC();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        } else if (RESOURCE_D.equals(name)) {
            Archive<?> archive = ResourceItemsTestBase.getResourceD();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        }
        return inputStream;
    }
}
