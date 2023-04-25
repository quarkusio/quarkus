package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * To support mappings that are not public
 */
public abstract class MappingsConfigBuilder implements ConfigBuilder {
    protected static void addMapping(SmallRyeConfigBuilder builder, String mappingClass, String prefix) {
        // TODO - Ideally should use the classloader passed to Config, but the method is not public. Requires a change in SmallRye Config.
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            builder.withMapping(contextClassLoader.loadClass(mappingClass), prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
