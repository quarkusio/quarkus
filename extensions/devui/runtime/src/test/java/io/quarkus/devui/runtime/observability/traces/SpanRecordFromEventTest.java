package io.quarkus.devui.runtime.observability.traces;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetrySignals;

public class SpanRecordFromEventTest {

    @Test
    public void aSpanComesBackAsTheRecordTheTracesViewShows() {
        TelemetryEvent event = TelemetryEvent.span("GET /hello")
                .traceId("t1").spanId("s1").parentSpanId("0000000000000000").kind("SERVER")
                .time(1_000L, 4_000L)
                .status("OK", "")
                .scope("io.quarkus.vertx.http").serviceName("app")
                .spanAttributes(Map.of("http.route", "/hello"))
                .events(List.of("exception @2000"))
                .build();

        SpanRecord span = SpanRecord.from(event);

        assertThat(span.traceId()).isEqualTo("t1");
        assertThat(span.spanId()).isEqualTo("s1");
        assertThat(span.parentSpanId()).isEqualTo("0000000000000000");
        assertThat(span.name()).isEqualTo("GET /hello");
        assertThat(span.kind()).isEqualTo("SERVER");
        assertThat(span.startEpochNanos()).isEqualTo(1_000L);
        assertThat(span.endEpochNanos()).isEqualTo(4_000L);
        assertThat(span.durationNanos()).isEqualTo(3_000L);
        assertThat(span.statusCode()).isEqualTo("OK");
        assertThat(span.scopeName()).isEqualTo("io.quarkus.vertx.http");
        assertThat(span.resourceServiceName()).isEqualTo("app");
        assertThat(span.attributes()).containsEntry("http.route", "/hello");
        assertThat(span.events()).containsExactly("exception @2000");
    }

    @Test
    public void aSpanWithMissingDetailsStillReads() {
        SpanRecord span = SpanRecord.from(new TelemetryEvent(TelemetrySignals.SPAN, "work", 1L, Map.of()));

        assertThat(span.name()).isEqualTo("work");
        assertThat(span.traceId()).isEmpty();
        assertThat(span.durationNanos()).isZero();
        assertThat(span.attributes()).isEmpty();
        assertThat(span.events()).isEmpty();
    }
}
