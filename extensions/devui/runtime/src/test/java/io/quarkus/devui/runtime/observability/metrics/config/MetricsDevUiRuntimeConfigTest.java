package io.quarkus.devui.runtime.observability.metrics.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * The metrics backends read the sample interval through {@link TelemetryEvents}, without depending on Dev UI; they
 * have to agree with the config that declares it on its name and on its default.
 */
public class MetricsDevUiRuntimeConfigTest {

    @Test
    public void theBackendsAndTheDashboardAgreeOnTheDefaultSampleInterval() {
        SmallRyeConfig config = config(Map.of());

        assertThat(config.getConfigMapping(MetricsDevUiRuntimeConfig.class).sampleInterval())
                .isEqualTo(TelemetryEvents.metricsSampleInterval(config::getOptionalValue));
    }

    @Test
    public void theBackendsReadTheSampleIntervalTheDashboardIsConfiguredWith() {
        SmallRyeConfig config = config(Map.of(TelemetryEvents.METRICS_SAMPLE_INTERVAL, "200ms"));

        assertThat(config.getConfigMapping(MetricsDevUiRuntimeConfig.class).sampleInterval())
                .isEqualTo(Duration.ofMillis(200));
        assertThat(TelemetryEvents.metricsSampleInterval(config::getOptionalValue)).isEqualTo(Duration.ofMillis(200));
    }

    private static SmallRyeConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMapping(MetricsDevUiRuntimeConfig.class)
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
    }
}
