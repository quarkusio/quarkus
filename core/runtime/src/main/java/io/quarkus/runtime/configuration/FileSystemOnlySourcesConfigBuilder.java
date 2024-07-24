package io.quarkus.runtime.configuration;

import static io.smallrye.config.PropertiesConfigSourceLoader.inFileSystem;

import java.nio.file.Paths;

import io.smallrye.config.SmallRyeConfigBuilder;

public class FileSystemOnlySourcesConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.setAddDefaultSources(false).addSystemSources().withSources(
                inFileSystem(Paths.get(System.getProperty("user.dir"), "config", "application.properties").toUri().toString(),
                        260, builder.getClassLoader()));
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

}
