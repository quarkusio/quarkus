package io.quarkus.extest.deployment;

import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class BuildTimeCustomConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        // for ConfigBuilderTest
        builder.withSources(
                new PropertiesConfigSource(Map.of("prop.recorded.from.btconfigsource", "1234"), "BuildTimeConfigSource", 100));
        // for RecorderRuntimeConfigTest
        builder.withSources(
                new PropertiesConfigSource(Map.of(
                        "recorded.property", "from-application",
                        "%test.recorded.profiled.property", "from-application",
                        "recorded.profiled.property", "recorded",
                        "%test.quarkus.mapping.rt.record-profiled", "from-application",
                        "quarkus.mapping.rt.record-profiled", "recorded"), "BuildTimeConfigSource", 250));
    }
}
