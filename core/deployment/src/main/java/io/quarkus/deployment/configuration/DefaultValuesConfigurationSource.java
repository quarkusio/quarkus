package io.quarkus.deployment.configuration;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class DefaultValuesConfigurationSource implements ConfigSource {
    private final ConfigPatternMap<LeafConfigType> leafs;

    public DefaultValuesConfigurationSource(final ConfigPatternMap<LeafConfigType> leafs) {
        this.leafs = leafs;
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    public String getValue(final String propertyName) {
        final LeafConfigType match = leafs.match(propertyName);
        return match == null ? null : match.getDefaultValueString();
    }

    public String getName() {
        return "default values";
    }

    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
