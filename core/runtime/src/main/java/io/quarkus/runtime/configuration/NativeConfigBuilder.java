package io.quarkus.runtime.configuration;

import static io.smallrye.config.PropertiesConfigSourceLoader.inFileSystem;

import java.nio.file.Paths;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Do not register classpath resources lookup in native mode to avoid missing resources registration errors, which
 * became a strict check during native image execution.
 * <p>
 * Configuration coming from classpath resources is recoded during build time in a low ordinal source, so the
 * configuration will still be available. If the users change the ordinals of the sources in runtime, it may
 * cause unexpected values to be returned by the config, but this has always been the case. The classpath configuration
 * resources were never registered in the native image.
 */
public class NativeConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder
                .setAddDefaultSources(false)
                .setAddSystemSources(true)
                .setAddPropertiesSources(false);

        builder.withSources(inFileSystem(
                Paths.get(System.getProperty("user.dir"), "config", "application.properties").toUri().toString(), 260,
                builder.getClassLoader()));

        return builder;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE + 100;
    }
}
