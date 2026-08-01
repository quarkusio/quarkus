package io.quarkus.micrometer.runtime.binder.grpc;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Timer;

class GrpcMetricTimerCustomizerTest {

    Timer.Builder builder;

    @BeforeEach
    void setUp() {
        builder = mock(Timer.Builder.class);
        when(builder.publishPercentileHistogram()).thenReturn(builder);
        when(builder.minimumExpectedValue(any())).thenReturn(builder);
        when(builder.maximumExpectedValue(any())).thenReturn(builder);
        when(builder.serviceLevelObjectives(any(Duration[].class))).thenReturn(builder);
    }

    @Test
    void histogramDisabledDoesNotCustomizeTimer() {
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(false,
                Optional.of(List.of(Duration.ofMillis(10))),
                Optional.of(Duration.ofMillis(1)),
                Optional.of(Duration.ofSeconds(1)));

        assertSame(builder, customizer.apply(builder));
        verifyNoInteractions(builder);
    }

    @Test
    void histogramEnabledUsesMicrometerPercentileHistogramDefaults() {
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, Optional.empty(),
                Optional.empty(), Optional.empty());

        assertSame(builder, customizer.apply(builder));
        verify(builder).publishPercentileHistogram();
        verify(builder, never()).minimumExpectedValue(any());
        verify(builder, never()).maximumExpectedValue(any());
        verify(builder, never()).serviceLevelObjectives(any(Duration[].class));
    }

    @Test
    void histogramEnabledAddsOptionalSloBuckets() {
        List<Duration> slos = List.of(Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1));
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, Optional.of(slos),
                Optional.empty(), Optional.empty());

        assertSame(builder, customizer.apply(builder));
        verify(builder).publishPercentileHistogram();
        verify(builder).serviceLevelObjectives(slos.toArray(Duration[]::new));
    }

    @Test
    void histogramEnabledAppliesExpectedValueOverrides() {
        Duration min = Duration.ofMillis(5);
        Duration max = Duration.ofMillis(100);
        UnaryOperator<Timer.Builder> customizer = GrpcMetricTimerCustomizer.create(true, Optional.empty(),
                Optional.of(min), Optional.of(max));

        assertSame(builder, customizer.apply(builder));
        verify(builder).publishPercentileHistogram();
        verify(builder).minimumExpectedValue(min);
        verify(builder).maximumExpectedValue(max);
    }
}
