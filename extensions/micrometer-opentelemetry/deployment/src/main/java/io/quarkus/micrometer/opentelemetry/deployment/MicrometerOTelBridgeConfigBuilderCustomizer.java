package io.quarkus.micrometer.opentelemetry.deployment;

import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class MicrometerOTelBridgeConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        // use a priority of 50 to make sure that this is overridable by any of the standard methods
        builder.withSources(
                new PropertiesConfigSource(Map.of(
                        "quarkus.otel.metrics.enabled", "true",
                        "quarkus.otel.logs.enabled", "true"), "quarkus-micrometer-opentelemetry", 1));
    }
}
