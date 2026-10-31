package io.quarkus.extest.runtime.config.unknown;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.common.MapBackedConfigSource;

public class UnknownBuildPropertyConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue value = context.getValue("skip.build.sources");
        if (value.getValue() == null || value.getValue().equals("false")) {
            return List.of(new MapBackedConfigSource("UnknownBuildPropertyConfigSource",
                    Map.of("quarkus.build-time.unknown.prop", "value",
                            // only the punctuation is different from quarkus.default-locale
                            "quarkus.default.locale", "en")) {
            },
                    // The Env Source also outputs the property names in lower case and with dots (for maps)
                    new EnvConfigSource(Map.of(
                            "QUARKUS_BUILD_TIME_UNKNOWN_ENV", "value",
                            "QUARKUS_HTTP_NON_APPLICATION_ROOT_PATH", "/1234",
                            "QUARKUS_HTTP_SSL_PORT", "4443"), EnvConfigSource.ORDINAL));
        } else {
            return emptyList();
        }
    }
}
