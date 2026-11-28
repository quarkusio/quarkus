package io.quarkus.test.opentelemetry;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

class SpanDataListTest {

    private static final String T1 = "000000000000000000000000000000a1";
    private static final String T2 = "000000000000000000000000000000a2";
    private static final String S1 = "00000000000000b1";
    private static final String S2 = "00000000000000b2";
    private static final String S3 = "00000000000000b3";
    private static final String S4 = "00000000000000b4";

    private final SpanData parent = span(T1, S1, SpanId.getInvalid(), "parent", INTERNAL);
    private final SpanData client = span(T1, S2, S1, "GET /hello", CLIENT);
    private final SpanData server = span(T1, S3, S2, "GET /hello", SERVER);
    private final SpanData other = span(T2, S4, SpanId.getInvalid(), "other", SERVER);

    private final SpanDataList trace = new SpanDataListImpl(List.of(parent, client, server));
    private final SpanDataList all = new SpanDataListImpl(List.of(parent, client, server, other));

    private static SpanData span(String traceId, String spanId, String parentSpanId, String name, SpanKind kind) {
        return TestSpanData.builder()
                .setName(name)
                .setKind(kind)
                .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
                .setParentSpanContext(SpanId.getInvalid().equals(parentSpanId)
                        ? SpanContext.getInvalid()
                        : SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault()))
                .setStatus(StatusData.unset())
                .setStartEpochNanos(1)
                .setEndEpochNanos(2)
                .setHasEnded(true)
                .build();
    }

    @Test
    void filters() {
        assertThat(all).hasSize(4);
        assertThat(all.ofKind(SERVER)).containsExactly(server, other);
        assertThat(all.named("GET /hello")).containsExactly(client, server);
        assertThat(all.roots()).containsExactly(parent, other);
        assertThat(all.childrenOf(parent)).containsExactly(client);
        assertThat(all.filter(span -> span.getSpanId().equals(S4))).containsExactly(other);
        assertThat(all.stream().map(SpanData::getName).distinct()).containsExactly("parent", "GET /hello", "other");
        assertThat(all.ofKind(CLIENT).childrenOf(parent).single()).isEqualTo(client);
        assertThat(all.named("missing")).isEmpty();
    }

    /**
     * A span with the same parent span id, in another trace, is not a child.
     */
    @Test
    void childrenAreInTheTraceOfTheirParent() {
        SpanData sameParentIdInAnotherTrace = span(T2, S2, S1, "impostor", CLIENT);

        assertThat(new SpanDataListImpl(List.of(parent, sameParentIdInAnotherTrace)).childrenOf(parent)).isEmpty();
    }

    /**
     * The assertions of OpenTelemetry group the spans by trace.
     */
    @Test
    void worksWithTheTraceAssertionsOfOpenTelemetry() {
        TracesAssert.assertThat(trace).hasSize(1);
        TracesAssert.assertThat(all).hasTracesSatisfyingExactly(
                first -> first.hasSpansSatisfyingExactlyInAnyOrder(
                        span -> span.hasName("parent").hasNoParent(),
                        span -> span.hasKind(CLIENT).hasParent(parent),
                        span -> span.hasKind(SERVER).hasParent(client)),
                second -> second.hasSpansSatisfyingExactly(span -> span.hasName("other")));
    }

    @Test
    void isUnmodifiable() {
        assertThatThrownBy(() -> all.add(parent)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(all::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void singleFailsUnlessExactlyOneSpan() {
        assertThatThrownBy(() -> all.ofKind(SERVER).single())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected exactly one span but found 2 span(s):")
                .hasMessageContaining("SERVER 'other' (trace " + T2 + ", span " + S4 + ", parent " + SpanId.getInvalid() + ")");
        assertThatThrownBy(() -> all.named("missing").single())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("found no spans");
    }
}
