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
import io.fabric8.api.AttributeKey.Factory;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.spi.DefaultContainerBuilder;
import io.fabric8.spi.management.ContainerOpenType;
import io.fabric8.spi.utils.ManagementUtils;
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

    static AttributeKey<String> AKEY = AttributeKey.create("AKey", String.class, new ValueFactory());
    static AttributeKey<String> BKEY = AttributeKey.create("BKey", String.class, new ValueFactory());

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

        DefaultContainerBuilder cntBuilder = DefaultContainerBuilder.create();
        cntBuilder.identityPrefix("cntA");
        cntBuilder.addAttribute(AKEY, "AVal");
        cntBuilder.addAttribute(BKEY, "BVal");
        CreateOptions options = cntBuilder.build();

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

        // Test the {@link CreateOptionsProvider}
        cntBuilder = DefaultContainerBuilder.create();
        cntBuilder.addOptions(new CompositeDataOptionsProvider(cdata));
        options = cntBuilder.build();

        Container cntC = cntManager.createContainer(options);
        ContainerIdentity idC = cntC.getIdentity();
        Assert.assertEquals("cntA#2", idC.getSymbolicName());
        Assert.assertEquals(cntA.getAttributes(), cntC.getAttributes());

        cntManager.destroyContainer(idC);
    }

    public static class ValueFactory implements Factory<String> {
        @Override
        public String createFrom(Object source) {
            return (String) source;
        }
    }

    static class CompositeDataOptionsProvider implements OptionsProvider<DefaultContainerBuilder> {

        private final CompositeData cdata;

        CompositeDataOptionsProvider(CompositeData cdata) {
            this.cdata = cdata;
        }

        @Override
        public DefaultContainerBuilder addBuilderOptions(DefaultContainerBuilder builder) {
            Container container = ContainerOpenType.getContainer(cdata);
            String symbolicName = container.getIdentity().getSymbolicName();
            String prefix = symbolicName.substring(0, symbolicName.indexOf('#'));
            return builder.identityPrefix(prefix).addAttributes(container.getAttributes());
        }
    }
}
