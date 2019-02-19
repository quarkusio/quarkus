package io.quarkus.runtime.configuration;

import java.util.Map;
import java.util.Set;

import io.smallrye.config.ConfigSourceMap;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;
import org.wildfly.common.annotation.NotNull;

/**
 * A base class for configuration sources which delegate to other configuration sources.
 */
public abstract class AbstractDelegatingConfigSource implements ConfigSource {
    protected final ConfigSource delegate;
    private Map<String, String> propertiesMap;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate configuration source (must not be {@code null})
     */
    public AbstractDelegatingConfigSource(final ConfigSource delegate) {
        Assert.checkNotNullParam("delegate", delegate);
        this.delegate = delegate;
    }

    /**
     * Get the delegate config source.
     *
     * @return the delegate config source (not {@code null})
     */
    protected @NotNull ConfigSource getDelegate() {
        return delegate;
    }

    public final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = this.propertiesMap;
        if (propertiesMap == null) {
            propertiesMap = this.propertiesMap = new ConfigSourceMap(this);
        }
        return propertiesMap;
    }

    public abstract Set<String> getPropertyNames();

    public String getName() {
        return delegate.getName();
    }
}
