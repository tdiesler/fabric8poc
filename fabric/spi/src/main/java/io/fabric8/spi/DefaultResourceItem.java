package io.fabric8.spi;

import io.fabric8.api.AttributeKey;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * The default resource item
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public final class DefaultResourceItem extends AbstractProfileItem implements ImportableResourceItem {

    private final InputStream inputStream;
    private final URL resourceURL;

    public DefaultResourceItem(String identity, Map<AttributeKey<?>, Object> attributes, InputStream inputStream) {
        super(identity, attributes);
        IllegalArgumentAssertion.assertNotNull(inputStream, "inputStream");
        this.inputStream = inputStream;
        this.resourceURL = null;
    }

    public DefaultResourceItem(String identity, Map<AttributeKey<?>, Object> attributes, URL resourceURL) {
        super(identity, attributes);
        IllegalArgumentAssertion.assertNotNull(resourceURL, "resourceURL");
        this.resourceURL = resourceURL;
        this.inputStream = null;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public URL getURL() {
        return resourceURL;
    }

    @Override
    public int hashCode() {
        return getIdentity().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof DefaultResourceItem)) return false;
        DefaultResourceItem other = (DefaultResourceItem) obj;
        return getIdentity().equals(other.getIdentity());
    }

    @Override
    public String toString() {
        return "ResourceItem[id=" + getIdentity() + ",url=" + resourceURL + "]";
    }
}