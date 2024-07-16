package io.quarkus.config.yaml.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

public class YamlConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new YamlConfigSourceLoader.InFileSystem())
                .withSources(new YamlConfigSourceLoader.InClassPath());
    }
}
