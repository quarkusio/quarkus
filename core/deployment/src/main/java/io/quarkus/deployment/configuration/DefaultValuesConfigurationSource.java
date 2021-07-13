package io.quarkus.deployment.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.deployment.configuration.matching.Container;

/**
 *
 */
public class DefaultValuesConfigurationSource implements ConfigSource {
    private final ConfigPatternMap<Container> leafs;

    public DefaultValuesConfigurationSource(final ConfigPatternMap<Container> leafs) {
        this.leafs = leafs;
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    public String getValue(final String propertyName) {
        final Container match = leafs.match(propertyName);
        if (match == null) {
            return null;
        }
        final ClassDefinition.ClassMember member = match.getClassMember();
        if (member instanceof ClassDefinition.ItemMember) {
            final ClassDefinition.ItemMember leafMember = (ClassDefinition.ItemMember) member;
            return leafMember.getDefaultValue();
        }
        return null;
    }

    public String getName() {
        return "default values";
    }

    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
