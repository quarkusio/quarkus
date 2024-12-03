package io.quarkus.config.yaml.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

/**
 * Only for JVM mode.
 *
 * @see io.quarkus.runtime.configuration.RuntimeConfigBuilder
 */
public class YamlInClassPathConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new YamlConfigSourceLoader.InClassPath());
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE + 100;
    }
}
