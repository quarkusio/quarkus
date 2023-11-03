package io.quarkus.it.smallrye.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class UserConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("user.config.provider.prop", "1234");
        return Collections.singleton(new UserConfigSource(properties));
    }
}
