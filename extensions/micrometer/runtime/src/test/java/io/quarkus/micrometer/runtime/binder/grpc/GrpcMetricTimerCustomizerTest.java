package io.quarkus.micrometer.runtime.binder.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GrpcMetricTimerCustomizerTest {

    @Test
    void histogramDisabledDoesNotPublishBuckets() {
        MeterRegistry registry = new SimpleMeterRegistry();
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(false,
                List.of(Duration.ofMillis(10), Duration.ofMillis(100)));

        Timer timer = customizer.apply(Timer.builder("grpc.server.processing.duration")).register(registry);
        timer.record(Duration.ofMillis(15));

        assertEquals(0, timer.takeSnapshot().histogramCounts().length);
    }

    @Test
    void histogramEnabledPublishesConfiguredSloBuckets() {
        MeterRegistry registry = new SimpleMeterRegistry();
        List<Duration> slos = List.of(Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1));
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, slos);

        Timer timer = customizer.apply(Timer.builder("grpc.server.processing.duration")).register(registry);
        timer.record(Duration.ofMillis(15));

        CountAtBucket[] buckets = timer.takeSnapshot().histogramCounts();
        assertTrue(buckets.length > 0);
        assertTrue(Arrays.stream(buckets).anyMatch(bucket -> bucket.bucket(TimeUnit.MILLISECONDS) == 10));
        assertTrue(Arrays.stream(buckets).anyMatch(bucket -> bucket.bucket(TimeUnit.MILLISECONDS) == 100));
        assertTrue(Arrays.stream(buckets).anyMatch(bucket -> bucket.bucket(TimeUnit.SECONDS) == 1));
    }
}
