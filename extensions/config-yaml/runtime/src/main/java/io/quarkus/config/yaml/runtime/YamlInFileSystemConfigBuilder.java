package io.quarkus.config.yaml.runtime;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceLoader;

public class YamlInFileSystemConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        // Removes Yaml source providers added by the ServiceLoader to avoid double registration
        List<ConfigSourceProvider> toRemove = new ArrayList<>();
        for (ConfigSourceProvider sourceProvider : builder.getSourceProviders()) {
            if (sourceProvider instanceof YamlConfigSourceLoader) {
                toRemove.add(sourceProvider);
            }
        }
        builder.getSourceProviders().removeAll(toRemove);
        return builder.withSources(new YamlConfigSourceLoader.InFileSystem());
    }
}
