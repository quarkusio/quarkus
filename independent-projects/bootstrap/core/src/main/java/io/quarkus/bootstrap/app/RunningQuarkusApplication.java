package io.quarkus.bootstrap.app;

import java.lang.annotation.Annotation;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;

public interface RunningQuarkusApplication extends AutoCloseable {
    ClassLoader getClassLoader();

    @Override
    void close() throws Exception;

    /**
     * @deprecated Should use {@link ValueRegistry}.
     */
    @Deprecated
    <T> Optional<T> getConfigValue(String key, Class<T> type);

    @Deprecated
    Iterable<String> getConfigKeys();

    /**
     * Looks up an instance from the CDI container of the running application.
     *
     * @param clazz The class
     * @param qualifiers The qualifiers
     * @return The instance or null
     */
    Object instance(Class<?> clazz, Annotation... qualifiers);

    /**
     * Gets this Application {@link ValueRegistry}.
     *
     * @return this Application {@link ValueRegistry}.
     */
    ValueRegistry valueRegistry();
}
