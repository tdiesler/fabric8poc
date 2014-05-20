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
import io.fabric8.test.embedded.support.EmbeddedUtils;
import io.fabric8.test.smoke.RequirementItemTestBase;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.jboss.gravia.resource.ResourceIdentity;
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
public class RequirementItemTest extends RequirementItemTestBase {

    private static Set<ResourceIdentity> repositoryIdentities;

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();

        // Add repository content
        String resname = "META-INF/repository-content/camel.core.feature.xml";
        URL resurl = RequirementItemTest.class.getClassLoader().getResource(resname);
        repositoryIdentities = EmbeddedUtils.addRepositoryContent(resurl);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedUtils.removeRepositoryContent(repositoryIdentities);
        EmbeddedTestSupport.afterClass();
    }

    @Override
    protected InputStream getDeployment(String name) {
        InputStream inputStream = null;
        if (RESOURCE_A.equals(name)) {
            Archive<?> archive = RequirementItemTestBase.getResourceA();
            inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        }
        return inputStream;
    }
}
