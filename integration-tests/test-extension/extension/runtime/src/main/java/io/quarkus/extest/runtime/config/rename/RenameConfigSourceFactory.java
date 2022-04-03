package io.quarkus.extest.runtime.config.rename;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;

public class RenameConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue value = context.getValue("skip.build.sources");
        if (value.getValue() == null || value.getValue().equals("false")) {
            return List.of(new RenameConfigSource());
        } else {
            return Collections.emptyList();
        }
    }
}
