package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.List;
import java.util.Set;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;

/**
 * {@link ExtendedDeclarativeConfigProperties} over plain {@link DeclarativeConfigProperties} and a
 * {@link ConfigProvider}. Upstream {@link ComponentProvider}s, such as the OTLP exporters, require the extended type,
 * which upstream's {@code DeclarativeConfigContext} passes to every component it creates. A Quarkus provider that
 * creates another component from one of its nested nodes wraps that node in this class, since the nested nodes are
 * not wrapped.
 */
public final class QuarkusExtendedConfigProperties implements ExtendedDeclarativeConfigProperties {

    private final DeclarativeConfigProperties delegate;
    private final ConfigProvider configProvider;

    /**
     * Wraps {@code delegate}.
     *
     * @param delegate the properties to read, not {@code null}
     * @param configProvider returned by {@link #getConfigProvider()}, not {@code null}
     */
    public QuarkusExtendedConfigProperties(DeclarativeConfigProperties delegate, ConfigProvider configProvider) {
        this.delegate = delegate;
        this.configProvider = configProvider;
    }

    @Override
    public ConfigProvider getConfigProvider() {
        return configProvider;
    }

    @Override
    public String getString(String name) {
        return delegate.getString(name);
    }

    @Override
    public Boolean getBoolean(String name) {
        return delegate.getBoolean(name);
    }

    @Override
    public Integer getInt(String name) {
        return delegate.getInt(name);
    }

    @Override
    public Long getLong(String name) {
        return delegate.getLong(name);
    }

    @Override
    public Double getDouble(String name) {
        return delegate.getDouble(name);
    }

    @Override
    public boolean isString(String name) {
        return delegate.isString(name);
    }

    @Override
    public boolean isBoolean(String name) {
        return delegate.isBoolean(name);
    }

    @Override
    public boolean isInt(String name) {
        return delegate.isInt(name);
    }

    @Override
    public boolean isLong(String name) {
        return delegate.isLong(name);
    }

    @Override
    public boolean isDouble(String name) {
        return delegate.isDouble(name);
    }

    @Override
    public <T> List<T> getScalarList(String name, Class<T> scalarType) {
        return delegate.getScalarList(name, scalarType);
    }

    @Override
    public DeclarativeConfigProperties getStructured(String name) {
        return delegate.getStructured(name);
    }

    @Override
    public List<DeclarativeConfigProperties> getStructuredList(String name) {
        return delegate.getStructuredList(name);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return delegate.getPropertyKeys();
    }

    @Override
    public ComponentLoader getComponentLoader() {
        return delegate.getComponentLoader();
    }
}
