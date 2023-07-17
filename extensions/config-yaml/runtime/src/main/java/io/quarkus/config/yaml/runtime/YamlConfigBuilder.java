package io.quarkus.config.yaml.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class YamlConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new ApplicationYamlConfigSourceLoader.InFileSystem())
                .withSources(new ApplicationYamlConfigSourceLoader.InClassPath());
    }
}
