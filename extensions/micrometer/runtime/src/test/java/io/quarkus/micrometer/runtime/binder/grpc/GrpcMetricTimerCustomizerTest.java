package io.quarkus.micrometer.runtime.binder.grpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

class GrpcMetricTimerCustomizerTest {

    @Test
    void histogramDisabledDoesNotPublishBuckets() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(false,
                Optional.of(List.of(Duration.ofMillis(10), Duration.ofMillis(100))));

        Timer timer = customizer.apply(Timer.builder("grpc.server.processing.duration")).register(registry);
        timer.record(Duration.ofMillis(15));

        assertFalse(registry.scrape().contains("grpc_server_processing_duration_seconds_bucket"));
    }

    @Test
    void histogramEnabledUsesMicrometerPercentileHistogramDefaults() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, Optional.empty());

        Timer timer = customizer.apply(Timer.builder("grpc.server.processing.duration")).register(registry);
        timer.record(Duration.ofMillis(15));

        String scrape = registry.scrape();
        assertTrue(scrape.contains("# TYPE grpc_server_processing_duration_seconds histogram"), scrape);
        assertTrue(scrape.contains("grpc_server_processing_duration_seconds_bucket{"), scrape);
        assertTrue(scrape.contains("le=\"+Inf\""), scrape);
    }

    @Test
    void histogramEnabledAddsOptionalSloBuckets() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        List<Duration> slos = List.of(Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1));
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, Optional.of(slos));

        Timer timer = customizer.apply(Timer.builder("grpc.server.processing.duration")).register(registry);
        timer.record(Duration.ofMillis(15));

        String scrape = registry.scrape();
        assertTrue(scrape.contains("le=\"0.01\""), scrape);
        assertTrue(scrape.contains("le=\"0.1\""), scrape);
        assertTrue(scrape.contains("le=\"1.0\""), scrape);
    }
}
