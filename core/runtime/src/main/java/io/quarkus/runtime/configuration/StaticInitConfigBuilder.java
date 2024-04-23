package io.quarkus.runtime.configuration;

import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * The initial configuration for Static Init.
 */
public class StaticInitConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        new QuarkusConfigBuilderCustomizer().configBuilder(builder);

        builder.forClassLoader(Thread.currentThread().getContextClassLoader())
                .addDefaultInterceptors()
                .addDefaultSources()
                .withSources(new ApplicationPropertiesConfigSourceLoader.InFileSystem())
                .withSources(new ApplicationPropertiesConfigSourceLoader.InClassPath())
                .withSources(new DotEnvConfigSourceProvider());
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
