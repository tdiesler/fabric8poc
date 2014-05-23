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
        // [TODO] Derive port from tomcat config
        // https://issues.jboss.org/browse/FABRIC-761
        properties.setProperty("org.osgi.service.http.port", "8080");
        return properties;
    }

}
