package io.quarkus.bootstrap.app;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface RunningQuarkusApplication extends AutoCloseable {
    ClassLoader getClassLoader();

    @Override
    void close() throws Exception;

    <T> Optional<T> getConfigValue(String key, Class<T> type);


    Iterable<String> getConfigKeys();

    /**
     * Looks up an instance from the CDI container of the running application.
     *
     * @param clazz The class
     * @param qualifiers The qualifiers
     * @return The instance or null
     */
    Object instance(Class<?> clazz, Annotation... qualifiers);
}
