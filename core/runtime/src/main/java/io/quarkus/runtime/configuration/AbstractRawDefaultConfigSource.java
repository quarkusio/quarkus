package io.quarkus.runtime.configuration;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The base class for the config source that yields the 'raw' default values.
 */
public abstract class AbstractRawDefaultConfigSource implements ConfigSource, Serializable {
    private static final long serialVersionUID = 2524612253582530249L;

    public static final String NAME = "default values";

    protected AbstractRawDefaultConfigSource() {
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    public String getValue(final String propertyName) {
        return getValue(new NameIterator(propertyName));
    }

    protected abstract String getValue(final NameIterator nameIterator);

    public String getName() {
        return NAME;
    }

    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
