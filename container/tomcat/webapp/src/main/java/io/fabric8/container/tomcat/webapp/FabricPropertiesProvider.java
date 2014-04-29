/*
 * #%L
 * Fabric8 :: Container :: Tomcat :: WebApp
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
package io.fabric8.container.tomcat.webapp;

import io.fabric8.spi.SystemProperties;

import java.io.File;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.jboss.gravia.container.tomcat.support.TomcatPropertiesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;

/**
 * The Fabric {@link PropertiesProvider}
 */
public class FabricPropertiesProvider extends TomcatPropertiesProvider {

    public FabricPropertiesProvider(ServletContext servletContext) {
        super(servletContext);
    }

    @Override
    protected Properties initialProperties(ServletContext servletContext) {
        Properties properties = super.initialProperties(servletContext);

        // Setup the karaf.home directory
        File karafBase = new File(getCatalinaWork().getPath() + File.separator + "karaf-base");
        File karafData = new File(karafBase.getPath() + File.separator + "data");
        File karafEtc = new File(karafBase.getPath() + File.separator + "etc");

        // [TODO] Derive port from tomcat config
        // https://issues.jboss.org/browse/FABRIC-761
        properties.setProperty("org.osgi.service.http.port", "8080");

        // Karaf integration properties
        properties.setProperty(SystemProperties.KARAF_HOME, karafBase.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_BASE, karafBase.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_DATA, karafData.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_ETC, karafEtc.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_NAME, "root");

        return properties;
    }

}
