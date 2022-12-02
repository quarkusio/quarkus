package io.quarkus.picocli.runtime;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSourceLoader;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.SmallRyeConfigBuilder;

public class PicocliConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        // Ideally we would create a new source builder, but it requires a lot of code to copy all required stuff, when we just want to remove a few sources
        List<ConfigSourceProvider> originalProviders = new ArrayList<>(builder.getSourceProviders());
        for (ConfigSourceProvider originalProvider : originalProviders) {
            if (originalProvider instanceof ApplicationPropertiesConfigSourceLoader.InClassPath) {
                builder.getSourceProviders().remove(originalProvider);
            } else if (originalProvider instanceof ApplicationPropertiesConfigSourceLoader.InFileSystem) {
                builder.getSourceProviders().remove(originalProvider);
            } else if (originalProvider instanceof DotEnvConfigSourceProvider) {
                builder.getSourceProviders().remove(originalProvider);
            }
        }
        return builder;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
