package io.quarkus.opentelemetry.deployment.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class TestSpanExporter implements SpanExporter {
    private final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();
    private volatile boolean isStopped = false;

    public static SpanData getSpanByKindAndParentId(List<SpanData> spans,
            SpanKind kind,
            Object parentSpanId) {
        List<SpanData> filteredSpans = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, filteredSpans.size(), "Received: " + spans);
        return filteredSpans.get(0);
    }

    public static List<SpanData> getSpansByKindAndParentId(List<SpanData> spans,
            SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.getKind().equals(kind))
                .filter(map -> map.getParentSpanId().equals(parentSpanId)).collect(toList());
    }

    /**
     * Careful when retrieving the list of finished spans. There is a chance when the response is already sent to the
     * client and Vert.x still writing the end of the spans. This means that a response is available to assert from the
     * test side but not all spans may be available yet. For this reason, this method requires the number of expected
     * spans.
     */
    public List<SpanData> getFinishedSpanItems(int spanCount) {
        assertSpanCount(spanCount);
        return finishedSpanItems.stream().collect(toList());
    }

    public void assertSpanCount(int spanCount) {
        await().atMost(30, SECONDS).untilAsserted(
                () -> assertEquals(spanCount, finishedSpanItems.size(), "Spans: " + finishedSpanItems.toString()));
    }

    public void reset() {
        finishedSpanItems.clear();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isStopped) {
            return CompletableResultCode.ofFailure();
        }
        finishedSpanItems.addAll(spans);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        finishedSpanItems.clear();
        isStopped = true;
        return CompletableResultCode.ofSuccess();
    }
}
