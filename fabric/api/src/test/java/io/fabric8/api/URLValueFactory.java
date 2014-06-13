package io.fabric8.api;

import io.fabric8.api.AttributeKey.ValueFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Simple URL {@link ValueFactory}
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Jun-2014
 */
class URLValueFactory implements ValueFactory<URL> {

    @Override
    public Class<URL> getType() {
        return URL.class;
    }

    @Override
    public URL createFrom(Object spec) {
        try {
            return new URL((String) spec);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}