package io.fabric8.spi;

import io.fabric8.api.Configuration;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.OptionsProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The default configuration item
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public final class DefaultConfigurationItemBuilder implements ConfigurationItemBuilder {

    private String identity;
    private final Map<String, Configuration> configurations = new LinkedHashMap<>();

    public DefaultConfigurationItemBuilder() {
    }

    public DefaultConfigurationItemBuilder(String identity) {
        this.identity = identity;
    }

    @Override
    public ConfigurationItemBuilder addOptions(OptionsProvider<ConfigurationItemBuilder> optionsProvider) {
        optionsProvider.addBuilderOptions(this);
        return this;
    }

    @Override
    public ConfigurationItemBuilder addIdentity(String identity) {
        this.identity = identity;
        return this;
    }

    @Override
    public ConfigurationItemBuilder addConfiguration(Map<String, Object> attributes) {
        DefaultConfiguration config = new DefaultConfiguration(Configuration.DEFAULT_MERGE_INDEX, attributes, null);
        configurations.put(config.getMergeIndex(), config);
        return this;
    }

    @Override
    public ConfigurationItemBuilder addConfiguration(String mergeIndex, Map<String, Object> attributes, Map<String, String> directives) {
        DefaultConfiguration config = new DefaultConfiguration(mergeIndex, attributes, directives);
        configurations.put(config.getMergeIndex(), config);
        return this;
    }

    @Override
    public ConfigurationItem getConfigurationItem() {
        DefaultConfigurationItem configItem = new DefaultConfigurationItem(identity, configurations);
        configItem.validate();
        return configItem;
    }

    static class DefaultConfigurationItem extends AbstractProfileItem implements ConfigurationItem {

        private final Map<String, Configuration> configurations = new LinkedHashMap<>();

        DefaultConfigurationItem(String identity, Map<String, Configuration> configs) {
            super(identity);
            configurations.putAll(configs);
        }

        @Override
        public Map<String, Object> getDefaultAttributes() {
            Configuration config = configurations.get(Configuration.DEFAULT_MERGE_INDEX);
            IllegalStateAssertion.assertNotNull(config, "Cannot obtain default attributes from: " + this);
            return config.getAttributes();
        }

        @Override
        public Configuration getConfiguration(String mergeIndex) {
            return configurations.get(mergeIndex);
        }

        @Override
        public List<Configuration> getConfigurations(Filter filter) {
            List<Configuration> result = new ArrayList<>();
            for (Configuration config : configurations.values()) {
                if (filter == null || filter.accept(config)) {
                    result.add(config);
                }
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        void validate() {
            super.validate();
            IllegalStateAssertion.assertFalse(configurations.isEmpty(), "No configurations associated");
            for (Configuration config : configurations.values()) {
                Map<String, Object> attributes = config.getAttributes();
                IllegalStateAssertion.assertFalse(attributes.isEmpty(), "No attributes define in: " + config);
            }
        }

        @Override
        public int hashCode() {
            return getIdentity().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof DefaultConfigurationItem))
                return false;
            DefaultConfigurationItem other = (DefaultConfigurationItem) obj;
            return getIdentity().equals(other.getIdentity());
        }

        @Override
        public String toString() {
            return "ConfigurationItem[id=" + getIdentity() + ",configs=" + configurations + "]";
        }
    }

    static class DefaultConfiguration implements Configuration {

        private final String mergeIndex;
        private final Map<String, Object> attributes;
        private final Map<String, String> directives;

        DefaultConfiguration(String mergeIndex, Map<String, Object> atts, Map<String, String> dirs) {
            this.mergeIndex = mergeIndex;
            attributes = atts != null ? new HashMap<String, Object>(atts) : Collections.<String, Object> emptyMap();
            directives = dirs != null ? new HashMap<String, String>(dirs) : Collections.<String, String> emptyMap();
        }

        @Override
        public String getMergeIndex() {
            return mergeIndex;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }

        @Override
        public Object getAttribute(String key) {
            return attributes.get(key);
        }

        @Override
        public Map<String, String> getDirectives() {
            return Collections.unmodifiableMap(directives);
        }

        @Override
        public String getDirective(String key) {
            return directives.get(key);
        }

        @Override
        public String toString() {
            return "[id=" + mergeIndex + ",atts=" + attributes + ",dirs=" + directives + "]";
        }
    }
}