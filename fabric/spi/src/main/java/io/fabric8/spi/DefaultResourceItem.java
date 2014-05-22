package io.fabric8.spi;

import io.fabric8.api.ResourceItem;

import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.ResourceUtils;

/**
 * The default resource item
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public final class DefaultResourceItem extends AbstractProfileItem implements ResourceItem {

    static final char[] ILLEGAL_IDENTITY_CHARS = new char[] {'/', '\\', ':', ' ', '\t', '&', '?'};

    private final Resource resource;

    public DefaultResourceItem(Resource resource) {
        super(resource.getIdentity().getCanonicalForm());
        IllegalArgumentAssertion.assertNotNull(resource, "resource");
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getSymbolicName() {
        return resource.getIdentity().getSymbolicName();
    }

    @Override
    public Version getVersion() {
        return resource.getIdentity().getVersion();
    }

    @Override
    public boolean isShared() {
        return ResourceUtils.isShared(resource);
    }

    void validate() {
        super.validate();
        String symbolicName = getSymbolicName();
        for (char ch : ILLEGAL_IDENTITY_CHARS) {
            IllegalStateAssertion.assertEquals(-1, symbolicName.indexOf(ch), "Invalid character '" + ch + "' in identity: " + getIdentity());
        }
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