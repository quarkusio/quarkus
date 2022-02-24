package io.quarkus.extest.runtime.config.unknown;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;

public class UnknownBuildPropertyConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue value = context.getValue("skip.build.sources");
        if (value.getValue() == null || value.getValue().equals("false")) {
            return List.of(new PropertiesConfigSource(Map.of("quarkus.build.unknown.prop", "value"), "", 100));
        } else {
            return emptyList();
        }
    }
}
