package io.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Provides a way to customize the {@link io.smallrye.config.SmallRyeConfig} used by Quarkus.
 * <br>
 * The {@link ConfigBuilder} must be registered with either <code>StaticInitConfigBuilderBuildItem</code> or
 * <code>RunTimeConfigBuilderBuildItem</code> (or both), to be applied.
 */
public interface ConfigBuilder {
    SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder);

    default int priority() {
        return 0;
    }
}
