package io.fabric8.spi;

import io.fabric8.api.ConfigurationProfileItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ImmutableConfigurationProfileItem extends AbstractProfileItem implements ConfigurationProfileItem {

    private final Map<String, Object> configuration = new HashMap<String, Object>();

    public ImmutableConfigurationProfileItem(String identity, Map<String, Object> configuration) {
        super(identity);
        this.configuration.putAll(configuration);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
    }

    @Override
    public int hashCode() {
        return getIdentity().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ImmutableConfigurationProfileItem)) return false;
        ImmutableConfigurationProfileItem other = (ImmutableConfigurationProfileItem) obj;
        return getIdentity().equals(other.getIdentity());
    }

    @Override
    public String toString() {
        return "ConfigurationItem[id=" + getIdentity() + ",config=" + configuration + "]";
    }
}