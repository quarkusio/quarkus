package io.quarkus.runtime.configuration;

import java.util.HashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;

import io.smallrye.config.PropertiesConfigSource;

/**
 * A configuration source that copies all the keys from another configuration source and then
 * drops any reference to it. Useful for handling configuration sources which have inefficient iteration
 * capabilities.
 */
public final class FastCachedConfigSource extends PropertiesConfigSource {
    private final String name;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate configuration source (must not be {@code null})
     *
     * @implNote The {@code delegate} configuration source is not referenced after this call.
     */
    public FastCachedConfigSource(final ConfigSource delegate) {
        super(new HashMap<>(Assert.checkNotNullParam("delegate", delegate.getProperties())), "ignored", delegate.getOrdinal());
        name = delegate.getName();
    }

    public String getName() {
        return name;
    }
}
