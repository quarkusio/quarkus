package io.quarkus.test.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.booleanArrayKey;
import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.doubleArrayKey;
import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longArrayKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringArrayKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class SpanWireFormatTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b7ad6b7169203331";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";

    private static List<SpanData> roundTrip(List<SpanData> spans) {
        return SpanWireFormat.decode(SpanWireFormat.encode(spans).encode());
    }

    /**
     * Every field of a span is given a value that differs from its default, and the span that comes back must be
     * equal to the one that went in.
     */
    @Test
    void spanSurvivesTheRoundTrip() {
        Attributes attributes = Attributes.builder()
                .put(AttributeKey.valueKey("complex"), Value.of(KeyValue.of("mixed", Value.of(
                        Value.of("nested"), Value.of(1L), Value.of(0.5), Value.of(true), Value.of(new byte[] { 3, 4 }),
                        Value.empty()))))
                .put(stringKey("http.route"), "/hello")
                .put(longKey("http.response.status_code"), 200L)
                .put(doubleKey("ratio"), 0.5d)
                .put(booleanKey("error"), true)
                .put(stringArrayKey("names"), List.of("a", "b"))
                .put(longArrayKey("numbers"), List.of(1L, 2L))
                .put(booleanArrayKey("flags"), List.of(true, false))
                .put(doubleArrayKey("ratios"), List.of(0.25d, 2d))
                .build();
        TraceState traceState = TraceState.builder().put("z", "first").put("a", "second").put("m", "third").build();
        SpanData span = TestSpanData.builder()
                .setName("GET /hello")
                .setKind(SpanKind.SERVER)
                .setSpanContext(SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), traceState))
                .setParentSpanContext(
                        SpanContext.createFromRemoteParent(TRACE_ID, PARENT_SPAN_ID, TraceFlags.getSampled(), traceState))
                .setStatus(StatusData.create(StatusCode.ERROR, "it failed"))
                .setStartEpochNanos(1_000)
                .setEndEpochNanos(3_500)
                .setHasEnded(true)
                .setAttributes(attributes)
                .setTotalAttributeCount(10)
                .setEvents(List.of(EventData.create(2_000, "retry", Attributes.of(longKey("attempt"), 2L), 3)))
                .setTotalRecordedEvents(4)
                .setLinks(List.of(LinkData.create(
                        SpanContext.create(TRACE_ID, PARENT_SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault()),
                        Attributes.of(stringKey("reason"), "batch"), 2)))
                .setTotalRecordedLinks(5)
                .setResource(Resource.create(Attributes.of(stringKey("service.name"), "my-service"), "https://schema/resource"))
                .setInstrumentationScopeInfo(InstrumentationScopeInfo.builder("io.quarkus.test")
                        .setVersion("1.2.3")
                        .setSchemaUrl("https://schema/scope")
                        .setAttributes(Attributes.of(stringKey("scope"), "attribute"))
                        .build())
                .build();

        assertThat(roundTrip(List.of(span))).containsExactly(span);
    }

    @Test
    void spanWithoutParentSurvivesTheRoundTrip() {
        SpanData span = rootSpan();
        List<SpanData> decoded = roundTrip(List.of(span));

        assertThat(decoded).containsExactly(span);
        assertThat(decoded.getFirst()).hasNoParent();
    }

    private static SpanData rootSpan() {
        return TestSpanData.builder()
                .setName("root")
                .setKind(SpanKind.INTERNAL)
                .setSpanContext(SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()))
                .setStatus(StatusData.unset())
                .setStartEpochNanos(1)
                .setEndEpochNanos(2)
                .setHasEnded(true)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "context, , b7ad6b7169203331",
            "context, 00000000000000000000000000000000, 0000000000000000",
            "parentContext, ,",
            "parentContext, bad, bad",
            "parentContext, 0af7651916cd43dd8448eb211c80319c, 0000000000000000",
            "parentContext, 00000000000000000000000000000000, b7ad6b7169203331",
            "link, 0af7651916cd43dd8448eb211c80319c, bad"
    })
    void rejectsInvalidIds(String field, String traceId, String spanId) {
        JsonArray wire = SpanWireFormat.encode(List.of(rootSpan()));
        JsonObject span = wire.getJsonObject(0);
        JsonObject context = span.getJsonObject("context").copy().put("traceId", traceId).put("spanId", spanId);
        if (traceId == null) {
            context.remove("traceId");
        }
        if (spanId == null) {
            context.remove("spanId");
        }
        if (field.equals("link")) {
            span.getJsonArray("links").add(new JsonObject().put("context", context)
                    .put("attributes", new JsonArray()).put("totalAttributeCount", 0));
        } else {
            span.put(field, context);
        }
        assertThatThrownBy(() -> SpanWireFormat.decode(wire.encode())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid span context");
    }

    /**
     * The exception itself cannot travel, but what OpenTelemetry recorded about it does.
     */
    @Test
    void recordedExceptionArrivesAsAnEventWithItsAttributes() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        SpanData decoded;
        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            Span span = tracerProvider.get("test").spanBuilder("failing").startSpan();
            span.recordException(new IllegalStateException("boom"), Attributes.of(stringKey("exception.message"), "reported"));
            span.addEvent("exception", Attributes.of(stringKey("exception.message"), "reported"));
            span.recordException(new IllegalStateException());
            span.end();
            // closing the tracer provider empties the exporter
            decoded = roundTrip(exporter.getFinishedSpanItems()).getFirst();
        }

        assertThat(decoded.getEvents()).hasSize(3);
        assertThat(decoded.getEvents().get(0)).isInstanceOfSatisfying(ExceptionEventData.class,
                event -> assertThat(event.getException()).hasMessage("boom"));
        assertThat(decoded.getEvents().get(1)).hasName("exception").isNotInstanceOf(ExceptionEventData.class);
        assertThat(decoded.getEvents().get(2)).isInstanceOfSatisfying(ExceptionEventData.class,
                event -> assertThat(event.getException().getMessage()).isNull());
        assertThat(decoded.getEvents().getFirst()).hasName("exception")
                .hasAttributesSatisfying(
                        attributes -> assertThat(attributes)
                                .containsEntry("exception.type", IllegalStateException.class.getName())
                                .containsEntry("exception.message", "reported"));
    }
}
