/*
 * #%L
 * Gravia :: Resolver
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
package io.fabric8.domain.agent.internal;

import io.fabric8.spi.scr.AbstractComponent;

import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.HttpMethod;
import org.jolokia.util.RequestType;

/**
 * The Jolokia {@link Restrictor} service
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Jun-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(Restrictor.class)
public final class JolokiaRestrictorService extends AbstractComponent implements Restrictor {

    private final Restrictor restrictor = new AllowAllRestrictor();

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
    }

    public boolean isHttpMethodAllowed(HttpMethod pMethod) {
        return restrictor.isHttpMethodAllowed(pMethod);
    }

    public boolean isTypeAllowed(RequestType pType) {
        return restrictor.isTypeAllowed(pType);
    }

    public boolean isAttributeReadAllowed(ObjectName pName, String pAttribute) {
        return restrictor.isAttributeReadAllowed(pName, pAttribute);
    }

    public boolean isAttributeWriteAllowed(ObjectName pName, String pAttribute) {
        return restrictor.isAttributeWriteAllowed(pName, pAttribute);
    }

    public boolean isOperationAllowed(ObjectName pName, String pOperation) {
        return restrictor.isOperationAllowed(pName, pOperation);
    }

    public boolean isRemoteAccessAllowed(String... pHostOrAddress) {
        return restrictor.isRemoteAccessAllowed(pHostOrAddress);
    }

    public boolean isOriginAllowed(String pOrigin, boolean pIsStrictCheck) {
        return restrictor.isOriginAllowed(pOrigin, pIsStrictCheck);
    }
}
