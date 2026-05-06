package io.quarkus.quickcli.runtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * A {@link ConfigSource} that exposes parsed QuickCLI command-line options as
 * SmallRye Config properties.
 * <p>
 * Registered early (empty) during config building, then populated after CLI
 * parsing in {@link QuickCliRunner}. Option names are mapped by stripping
 * leading dashes from the longest option name (e.g. {@code --server.port}
 * becomes {@code server.port}).
 */
public class QuickCliConfigSource implements ConfigSource {

    static final String NAME = "QuickCLI CLI Options";
    static final int ORDINAL = 275;

    private static final Map<String, String> properties = new ConcurrentHashMap<>();

    public static void setProperties(Map<String, String> values) {
        properties.clear();
        properties.putAll(values);
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.copyOf(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
