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
package io.fabric8.test.smoke.container;

import io.fabric8.api.Container;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.test.smoke.HttpServiceTestBase;
import io.fabric8.test.smoke.PrePostConditions;

import java.io.InputStream;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.jboss.test.gravia.itests.support.HttpRequest;
import org.junit.runner.RunWith;
import org.osgi.service.http.HttpService;

/**
 * Test the {@link HttpService}
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2013
 */
@RunWith(Arquillian.class)
public class HttpServiceTest extends HttpServiceTestBase {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final RuntimeType targetContainer = ArchiveBuilder.getTargetContainer();
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "http-service.war");
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(HttpRequest.class, HttpServiceTestBase.class, PrePostConditions.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (targetContainer == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName("http-service");
                    builder.addBundleVersion("1.0.0");
                    builder.addImportPackages(RuntimeLocator.class, Resource.class, Container.class);
                    builder.addImportPackages(Servlet.class, HttpServlet.class, HttpService.class);
                    builder.addImportPackages(BootstrapComplete.class);
                    builder.addBundleClasspath("WEB-INF/classes");
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability("http-service", "1.0.0");
                    builder.addManifestHeader("Dependencies", "io.fabric8.api,io.fabric8.spi,org.osgi.enterprise");
                    return builder.openStream();
                }
            }
        });
        return archive;
    }
}
