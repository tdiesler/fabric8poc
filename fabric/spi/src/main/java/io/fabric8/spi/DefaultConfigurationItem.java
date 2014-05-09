package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class DefaultConfigurationItem extends AbstractProfileItem implements ConfigurationItem {

    private final Map<String, Object> configuration = new HashMap<String, Object>();

    public DefaultConfigurationItem(String identity, Map<AttributeKey<?>, Object> attributes, Map<String, Object> configuration) {
        super(identity, attributes);
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
        if (!(obj instanceof DefaultConfigurationItem)) return false;
        DefaultConfigurationItem other = (DefaultConfigurationItem) obj;
        return getIdentity().equals(other.getIdentity());
    }

    @Override
    public String toString() {
        return "ConfigurationItem[id=" + getIdentity() + ",config=" + configuration + "]";
    }
}