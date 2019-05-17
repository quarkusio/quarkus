package io.quarkus.runtime.execution;

import java.util.Map;
import java.util.Optional;

import org.wildfly.common.Assert;

import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The common base class of the build and run time configuration contexts.
 */
public abstract class ConfigurationExecutionContext extends ExecutionContext {

    private final Map<Class<?>, Object> configObjects;

    ConfigurationExecutionContext(final ExecutionContext parent, final Map<Class<?>, Object> configObjects) {
        super(parent);
        this.configObjects = configObjects;
    }

    /**
     * Get the configuration root of the given type.
     *
     * @param type the configuration type class (must not be {@code null})
     * @param <T> the configuration type
     * @return the populated configuration root object
     * @throws IllegalStateException if the given configuration root type is not available
     */
    public <T> T getConfigRoot(Class<T> type) throws IllegalStateException {
        final Object configObj = getRawConfigObject(type);
        if (configObj == null) {
            throw new IllegalStateException("Configuration " + type + " is not available");
        }
        return type.cast(configObj);
    }

    /**
     * Get an optional configuration root of the given type.
     *
     * @param type the configuration type class (must not be {@code null})
     * @param <T> the configuration type
     * @return the optionally populated configuration root object
     */
    public <T> Optional<T> getOptionalConfigRoot(Class<T> type) {
        return Optional.ofNullable(type.cast(getRawConfigObject(type)));
    }

    Object getRawConfigObject(final Class<?> type) {
        Assert.checkNotNullParam("type", type);
        // TODO: maybe we should make this a real run time check?
        assert type.getAnnotation(ConfigRoot.class) != null && type.getAnnotation(ConfigRoot.class).phase().isAvailableAtRun();
        return configObjects.get(type);
    }
}
