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

import io.fabric8.test.smoke.ResourceItemTestBase;

import java.io.InputStream;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.runner.RunWith;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@RunWith(Arquillian.class)
public class ResourceItemTest extends ResourceItemTestBase {

    @Override
    protected InputStream getDeployment(String name) {
        Archive<?> archive = null;
        if (RESOURCE_A.equals(name)) {
            archive = ResourceItemTestBase.getResourceA();
        } else if (RESOURCE_B.equals(name)) {
            archive = ResourceItemTestBase.getResourceB();
        } else if (RESOURCE_B1.equals(name)) {
            archive = ResourceItemTestBase.getResourceB1();
        } else if (RESOURCE_C.equals(name)) {
            archive = ResourceItemTestBase.getResourceC();
        } else if (CONTENT_F1.equals(name)) {
            archive = ResourceItemTestBase.getContentF1();
        } else if (CONTENT_F2.equals(name)) {
            archive = ResourceItemTestBase.getContentF2();
        } else if (CONTENT_F3.equals(name)) {
            archive = ResourceItemTestBase.getContentF3();
        } else if (CONTENT_G1.equals(name)) {
            archive = ResourceItemTestBase.getContentG1();
        } else if (CONTENT_G2.equals(name)) {
            archive = ResourceItemTestBase.getContentG2();
        } else if (CONTENT_G3.equals(name)) {
            archive = ResourceItemTestBase.getContentG3();
        }
        return archive.as(ZipExporter.class).exportAsInputStream();
    }
}
