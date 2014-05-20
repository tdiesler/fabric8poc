package io.fabric8.spi;

import io.fabric8.api.ResourceItem;

import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * The default resource item
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public final class DefaultResourceItem extends AbstractProfileItem implements ResourceItem {

    private final Resource resource;
    private final boolean shared;

    public DefaultResourceItem(Resource resource, boolean shared) {
        super(resource.getIdentity().getSymbolicName());
        IllegalArgumentAssertion.assertNotNull(resource, "resource");
        this.resource = resource;
        this.shared = shared;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public boolean isShared() {
        return shared;
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
        return "ResourceItem[id=" + getIdentity() + "]";
    }
}