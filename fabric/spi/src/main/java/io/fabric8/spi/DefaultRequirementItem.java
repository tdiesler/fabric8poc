package io.fabric8.spi;

import io.fabric8.api.RequirementItem;

import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;

/**
 * The default requirement item
 *
 * @author thomas.diesler@jboss.com
 * @since 15-May-2014
 */
public final class DefaultRequirementItem extends AbstractProfileItem implements RequirementItem {

    private final Requirement requirement;

    public DefaultRequirementItem(Requirement requirement) {
        super((String) requirement.getAttribute(IdentityNamespace.IDENTITY_NAMESPACE));
        this.requirement = requirement;
    }

    @Override
    public Requirement getRequirement() {
        return requirement;
    }

    @Override
    public int hashCode() {
        return getIdentity().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof DefaultRequirementItem)) return false;
        DefaultRequirementItem other = (DefaultRequirementItem) obj;
        return getIdentity().equals(other.getIdentity());
    }

    @Override
    public String toString() {
        return "RequirementItem[id=" + getIdentity() + "]";
    }
}