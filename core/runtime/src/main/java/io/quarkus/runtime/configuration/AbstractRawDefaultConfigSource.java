package io.quarkus.runtime.configuration;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The base class for the config source that yields the 'raw' default values.
 */
public abstract class AbstractRawDefaultConfigSource implements ConfigSource {
    protected AbstractRawDefaultConfigSource() {
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    public String getValue(final String propertyName) {
        return getValue(new NameIterator(propertyName));
    }

    protected abstract String getValue(final NameIterator nameIterator);

    public String getName() {
        return "default values";
    }

    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
