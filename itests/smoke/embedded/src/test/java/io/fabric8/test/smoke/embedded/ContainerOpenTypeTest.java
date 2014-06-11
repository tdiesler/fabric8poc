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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.spi.management.ContainerOpenType;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.test.embedded.support.EmbeddedContainerBuilder;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.openmbean.CompositeData;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test the {@link ProfileVersion}.
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 */
public class ContainerOpenTypeTest {

    static AttributeKey<String> AKEY = AttributeKey.create("AKey", String.class);
    static AttributeKey<String> BKEY = AttributeKey.create("BKey", String.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Test
    public void testComposisteData() throws Exception {

        CreateOptions options = EmbeddedContainerBuilder.create("cntA")
            .addAttribute(AKEY, "AVal")
            .addAttribute(BKEY, "BVal")
            .getCreateOptions();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cntA = cntManager.createContainer(options);
        ContainerIdentity idA = cntA.getIdentity();

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ContainerManagement cntManagement = ManagementUtils.getMBeanProxy(mbeanServer, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
        CompositeData cdata = cntManagement.getContainer(idA.getCanonicalForm());
        Container cntB = ContainerOpenType.getContainer(cdata);
        Assert.assertEquals(idA, cntB.getIdentity());
        Assert.assertEquals(cntA.getAttributes(), cntB.getAttributes());

        cntManager.destroyContainer(idA);

        // Test the {@link CompositeDataOptionsProvider}
        EmbeddedContainerBuilder cntBuilder = EmbeddedContainerBuilder.create();
        cntBuilder.addOptions(new CompositeDataOptionsProvider(cdata));
        options = cntBuilder.getCreateOptions();

        Container cntC = cntManager.createContainer(options);
        ContainerIdentity idC = cntC.getIdentity();
        Assert.assertEquals("cntA", idC.getCanonicalForm());
        Assert.assertEquals(cntA.getAttributes(), cntC.getAttributes());

        cntManager.destroyContainer(idC);
    }

    static class CompositeDataOptionsProvider implements OptionsProvider<EmbeddedContainerBuilder> {

        private final CompositeData cdata;

        CompositeDataOptionsProvider(CompositeData cdata) {
            this.cdata = cdata;
        }

        @Override
        public EmbeddedContainerBuilder addBuilderOptions(EmbeddedContainerBuilder builder) {
            Container container = ContainerOpenType.getContainer(cdata);
            String symbolicName = container.getIdentity().getSymbolicName();
            return builder.identity(symbolicName).addAttributes(container.getAttributes());
        }
    }
}
