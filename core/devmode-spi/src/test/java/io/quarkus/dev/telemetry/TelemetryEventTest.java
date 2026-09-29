package io.quarkus.dev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

public class TelemetryEventTest {

    @Test
    public void anySignalIsAccepted() {
        TelemetryEvent event = new TelemetryEvent("llm.call", "chat", 1L, Map.of("model", "m"));

        assertThat(event.signal()).isEqualTo("llm.call");
        assertThat(event.attributes()).containsEntry("model", "m");
    }

    @Test
    public void jsonCompatibleValuesAreAcceptedIncludingNullsAndNesting() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("missing", null);
        nested.put("flags", Arrays.asList(true, null, 3));

        TelemetryEvent event = new TelemetryEvent("s", "e", 1L, Map.of("nested", nested, "count", 7L));

        assertThat(event.attributes().get("nested")).isEqualTo(nested);
    }

    @Test
    public void aValueThatIsNotJsonIsRejectedWhereTheEventIsMade() {
        assertThatThrownBy(() -> new TelemetryEvent("s", "e", 1L, Map.of("when", Instant.EPOCH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attributes.when")
                .hasMessageContaining("java.time.Instant");

        assertThatThrownBy(() -> new TelemetryEvent("s", "e", 1L, Map.of("list", List.of(Instant.EPOCH))))
                .hasMessageContaining("attributes.list[0]");

        assertThatThrownBy(() -> new TelemetryEvent("s", "e", 1L, Map.of("map", Map.of(1, "x"))))
                .hasMessageContaining("keys of attributes.map must be strings");
    }

    @Test
    public void anEventNeedsASignalAndAName() {
        assertThatThrownBy(() -> new TelemetryEvent(" ", "n", 0, null)).hasMessageContaining("signal");
        assertThatThrownBy(() -> new TelemetryEvent("s", null, 0, null)).hasMessageContaining("name");
    }

    @Test
    public void theEventDoesNotChangeWhenTheSendersMapDoes() {
        Map<String, Object> attributes = new HashMap<>();
        List<Object> items = new ArrayList<>(List.of("a"));
        attributes.put("items", items);

        TelemetryEvent event = new TelemetryEvent("s", "e", 1L, attributes);
        attributes.put("late", "x");
        items.add("b");

        assertThat(event.attributes()).doesNotContainKey("late");
        assertThat(event.attributes().get("items")).isEqualTo(List.of("a"));
        assertThatThrownBy(() -> event.attributes().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void aMutableNumberIsKeptAsTheValueItHadWhenTheEventWasMade() {
        AtomicLong count = new AtomicLong(3);
        LongAdder adder = new LongAdder();
        adder.add(4);
        DoubleAdder sum = new DoubleAdder();
        sum.add(1.5);

        TelemetryEvent event = new TelemetryEvent("s", "e", 1L,
                Map.of("count", count, "adder", adder, "sum", sum, "nested", List.of(new AtomicLong(5))));
        count.set(30);
        adder.add(40);
        sum.add(15);

        assertThat(event.attributes())
                .containsEntry("count", 3L)
                .containsEntry("adder", 4L)
                .containsEntry("sum", 1.5)
                .containsEntry("nested", List.of(5L));
    }

    @Test
    public void aNumberThatIsNotFiniteBecomesNull() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("nan", Double.NaN);
        attributes.put("infinity", Float.POSITIVE_INFINITY);
        attributes.put("list", Arrays.asList(1.0, Double.NEGATIVE_INFINITY));

        TelemetryEvent event = new TelemetryEvent("s", "e", 1L, attributes);

        assertThat(event.attributes())
                .containsEntry("nan", null)
                .containsEntry("infinity", null)
                .containsEntry("list", Arrays.asList(1.0, null));
    }

    @Test
    public void anEventTakesAnotherEventsAttributesWithoutCopyingThem() {
        TelemetryEvent event = new TelemetryEvent("s", "e", 1L, Map.of("a", "b"));

        assertThat(new TelemetryEvent("s", "f", 2L, event.attributes()).attributes()).isSameAs(event.attributes());
    }

    @Test
    public void whatABuilderBuiltDoesNotChangeWithTheBuilderOrTheSendersMaps() {
        Map<String, String> tags = new HashMap<>(Map.of("uri", "/a"));
        MetricEventBuilder builder = TelemetryEvent.metric("m").value(1).tags(tags);
        TelemetryEvent event = builder.build();
        tags.put("uri", "/b");
        builder.unit("s");

        assertThat(event.attributes())
                .containsEntry(TelemetryAttributes.TAGS, Map.of("uri", "/a"))
                .doesNotContainKey(TelemetryAttributes.UNIT);
        assertThatThrownBy(() -> event.attributes().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        @SuppressWarnings("unchecked")
        Map<String, String> eventTags = (Map<String, String>) event.attributes().get(TelemetryAttributes.TAGS);
        assertThatThrownBy(() -> eventTags.put("x", "y")).isInstanceOf(UnsupportedOperationException.class);

        List<String> events = new ArrayList<>(List.of("a"));
        TelemetryEvent span = TelemetryEvent.span("s").events(events).build();
        events.add("b");
        assertThat(span.attributes()).containsEntry(TelemetryAttributes.EVENTS, List.of("a"));
    }

    @Test
    public void aBuilderLeavesOutWhatJsonCannotRepresent() {
        TelemetryEvent event = TelemetryEvent.metric("m")
                .distribution(1, 2)
                .percentiles(new double[] { Double.NaN }, new double[] { 1 })
                .buckets(new double[] { 1, Double.POSITIVE_INFINITY }, new double[] { 1, 2, 3 })
                .build();

        Map<String, Object> percentile = new HashMap<>();
        percentile.put("rank", null);
        percentile.put("value", 1.0);
        assertThat(event.attributes().get(TelemetryAttributes.PERCENTILES)).isEqualTo(List.of(percentile));
        assertThat(event.attributes()).containsEntry(TelemetryAttributes.BUCKET_BOUNDARIES, Arrays.asList(1.0, null));
    }

    @Test
    public void theSampleIntervalDefaultsWhenItIsNotConfigured() {
        assertThat(TelemetryEvents.metricsSampleInterval((key, type) -> Optional.empty()))
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(TelemetryEvents.metricsSampleInterval(
                (key, type) -> key.equals(TelemetryEvents.METRICS_SAMPLE_INTERVAL)
                        ? Optional.of(Duration.ofMillis(200))
                        : Optional.empty()))
                .isEqualTo(Duration.ofMillis(200));
    }

    @Test
    public void aMetricCarriesItsReadingAndDistribution() {
        TelemetryEvent event = TelemetryEvent.metric("http.server.requests")
                .value(3).type("TIMER").cumulative(true).unit("s").source("micrometer")
                .tags(Map.of("uri", "/hello"))
                .timestamp(1234L)
                .distribution(0.9, 0.5)
                .percentiles(new double[] { 0.5, 0.99 }, new double[] { 0.2, 0.5 })
                .buckets(new double[] { 0.1, 1 }, new double[] { 1, 2, 0 })
                .build();

        assertThat(event.signal()).isEqualTo(TelemetrySignals.METRIC);
        assertThat(event.name()).isEqualTo("http.server.requests");
        assertThat(event.timestamp()).isEqualTo(1234L);
        assertThat(event.attributes())
                .containsEntry(TelemetryAttributes.VALUE, 3.0)
                .containsEntry(TelemetryAttributes.TYPE, "TIMER")
                .containsEntry(TelemetryAttributes.CUMULATIVE, true)
                .containsEntry(TelemetryAttributes.UNIT, "s")
                .containsEntry(TelemetryAttributes.SOURCE, "micrometer")
                .containsEntry(TelemetryAttributes.TAGS, Map.of("uri", "/hello"))
                .containsEntry(TelemetryAttributes.TOTAL, 0.9)
                .containsEntry(TelemetryAttributes.MAX, 0.5)
                .containsEntry(TelemetryAttributes.BUCKET_BOUNDARIES, List.of(0.1, 1.0))
                .containsEntry(TelemetryAttributes.BUCKET_COUNTS, List.of(1.0, 2.0, 0.0));
        assertThat(event.attributes().get(TelemetryAttributes.PERCENTILES))
                .isEqualTo(List.of(Map.of("rank", 0.5, "value", 0.2), Map.of("rank", 0.99, "value", 0.5)));
    }

    @Test
    public void aReadingThatIsNotFiniteIsLeftOutRatherThanCarriedAsNaN() {
        TelemetryEvent event = TelemetryEvent.metric("gauge").value(Double.NaN).distribution(1, Double.NaN).build();

        assertThat(event.attributes())
                .doesNotContainKey(TelemetryAttributes.VALUE)
                .doesNotContainKey(TelemetryAttributes.MAX)
                .containsEntry(TelemetryAttributes.TOTAL, 1.0);
    }

    @Test
    public void aSpanIsStampedWithItsEnd() {
        TelemetryEvent event = TelemetryEvent.span("GET /hello")
                .traceId("t").spanId("s").parentSpanId("p").kind("SERVER")
                .time(1_000_000_000L, 3_000_000_000L)
                .status("OK", null)
                .scope("io.quarkus.vertx.http").serviceName("app")
                .spanAttributes(Map.of("http.route", "/hello"))
                .events(List.of("exception @5"))
                .build();

        assertThat(event.signal()).isEqualTo(TelemetrySignals.SPAN);
        assertThat(event.timestamp()).isEqualTo(3_000L);
        assertThat(event.attributes())
                .containsEntry(TelemetryAttributes.TRACE_ID, "t")
                .containsEntry(TelemetryAttributes.START_EPOCH_NANOS, 1_000_000_000L)
                .containsEntry(TelemetryAttributes.STATUS_DESCRIPTION, null)
                .containsEntry(TelemetryAttributes.SPAN_ATTRIBUTES, Map.of("http.route", "/hello"))
                .containsEntry(TelemetryAttributes.EVENTS, List.of("exception @5"));
    }

    @Test
    public void eventsAreOnlyEnabledOnceTheReceiverSaysSo() {
        try {
            TelemetryEvents.setEnabled(false);
            assertThat(TelemetryEvents.isEnabled()).isFalse();
            TelemetryEvents.setEnabled(true);
            assertThat(TelemetryEvents.isEnabled()).isTrue();
        } finally {
            TelemetryEvents.setEnabled(false);
        }
    }
}
