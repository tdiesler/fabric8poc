/*
 * #%L
 * Fabric8 :: Core
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
package io.fabric8.core.internal;

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.Profile;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;
import java.net.URLStreamHandler;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.Constants;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;

/**
 * Initial bootstrap of the system
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(BootstrapService.class)
public final class BootstrapService extends AbstractComponent {

    @Reference(referenceInterface = ConfigurationManager.class)
    private final ValidatingReference<ConfigurationManager> configurationManager = new ValidatingReference<ConfigurationManager>();
    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        for(ServiceRegistration<?> sreg : registrations) {
            sreg.unregister();
        }
    }

    private void activateInternal() throws IOException {

        // Register the URLStreamHandler services
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.URL_HANDLER_PROTOCOL, ProfileURLStreamHandler.PROTOCOL_NAME);
        ModuleContext syscontext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        registrations.add(syscontext.registerService(URLStreamHandler.class, new ProfileURLStreamHandler(), props));
        props.put(Constants.URL_HANDLER_PROTOCOL, ContainerURLStreamHandler.PROTOCOL_NAME);
        registrations.add(syscontext.registerService(URLStreamHandler.class, new ContainerURLStreamHandler(), props));

        // Apply default {@link ConfigurationProfileItem}s
        Profile profile = profileService.get().getDefaultProfile();
        List<ConfigurationItem> items = profile.getProfileItems(ConfigurationItem.class);
        configurationManager.get().applyConfigurationItems(items);
    }

    void bindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.bind(service);
    }

    void unbindConfigurationManager(ConfigurationManager service) {
        this.configurationManager.unbind(service);
    }

    void bindProfileService(ProfileService service) {
        this.profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        this.profileService.unbind(service);
    }
}
