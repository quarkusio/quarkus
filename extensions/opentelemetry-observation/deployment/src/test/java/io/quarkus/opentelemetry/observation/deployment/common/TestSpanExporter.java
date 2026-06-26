package io.quarkus.opentelemetry.observation.deployment.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class TestSpanExporter implements SpanExporter {

    private final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();
    private volatile boolean isStopped = false;

    public List<SpanData> getFinishedSpanItems(int spanCount) {
        assertSpanCount(spanCount);
        return finishedSpanItems.stream().collect(toList());
    }

    public void assertSpanCount(int spanCount) {
        await().atMost(5, SECONDS).untilAsserted(
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
