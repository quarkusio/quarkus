package io.quarkus.opentelemetry.runtime.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.dev.telemetry.TelemetryEvents;

public class DevUiMetricsExporterTest {

    @BeforeEach
    void enable() {
        TelemetryEvents.setEnabled(true);
    }

    @AfterEach
    void disable() {
        TelemetryEvents.setEnabled(false);
    }

    @Test
    public void aMetricThatCannotBeSentDoesNotCostTheOthersTheirReading() {
        RecordingEvent telemetry = new RecordingEvent("boom");

        new DevUiMetricsExporter(telemetry).export(List.of(counter("before"), counter("boom"), counter("after")));

        assertThat(telemetry.names).containsExactly("before", "after");
    }

    private static MetricData counter(String name) {
        return ImmutableMetricData.createLongSum(Resource.empty(), InstrumentationScopeInfo.create("test"), name, "",
                "1", ImmutableSumData.create(true, AggregationTemporality.CUMULATIVE,
                        List.of(ImmutableLongPointData.create(0, 1_000_000, Attributes.empty(), 1))));
    }
}
