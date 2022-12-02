package io.quarkus.jwt.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.common.MapBackedConfigSource;

public class PublicKeyLocationBuildTimeConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        // This property is only available in runtime.
        ConfigValue value = context.getValue("quarkus.uuid");
        if (value == null || value.getValue() == null) {
            return List.of(new MapBackedConfigSource(PublicKeyLocationBuildTimeConfigSourceFactory.class.getName(),
                    Map.of("mp.jwt.verify.publickey.location", "${invalid}"), 1000) {
            });
        }
        return Collections.emptyList();
    }
}
