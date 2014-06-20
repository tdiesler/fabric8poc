/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.spi;

import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointFactory;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

import java.util.ServiceLoader;

public final class ServiceEndpointFacotryLocator {

    @SuppressWarnings("unchecked")
    public static <T extends ServiceEndpoint> ServiceEndpointFactory<T> getFactory(Class<T> type) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        for (ServiceEndpointFactory<T> factory : ServiceLoader.load(ServiceEndpointFactory.class, type.getClassLoader())) {
            try {
                Class<?> targetType = factory.getClass().getMethod("create", ServiceEndpoint.class).getReturnType();
                if (type.isAssignableFrom(targetType)) {
                    return factory;
                }
            } catch (NoSuchMethodException e) {
                //Ignore and move to the next.
            }
        }
        throw new IllegalStateException("No ServiceEndpointFactory available for :" + type.getCanonicalName() + ".");
    }

    public static <T extends ServiceEndpoint> boolean isSupported(ServiceEndpoint serviceEndpoint, Class<T> type) {
        return getFactory(type).isSupported(serviceEndpoint);
    }

    public static <T extends ServiceEndpoint> T convert(ServiceEndpoint serviceEndpoint, Class<T> type) {
        if (isSupported(serviceEndpoint, type)) {
            return getFactory(type).create(serviceEndpoint);
        } else {
            throw new IllegalArgumentException("Converting "+ serviceEndpoint+" to "+ type+" is not supported");
        }
    }
}
