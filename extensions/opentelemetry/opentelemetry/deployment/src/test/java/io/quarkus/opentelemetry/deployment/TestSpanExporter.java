package io.quarkus.opentelemetry.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class TestSpanExporter implements SpanExporter {
    private final List<SpanData> finishedSpanItems = new ArrayList<>();
    private boolean isStopped = false;

    public List<SpanData> getFinishedSpanItems() {
        synchronized (this) {
            return Collections.unmodifiableList(new ArrayList<>(finishedSpanItems));
        }
    }

    public void reset() {
        synchronized (this) {
            finishedSpanItems.clear();
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        synchronized (this) {
            if (isStopped) {
                return CompletableResultCode.ofFailure();
            }
            finishedSpanItems.addAll(spans);
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        synchronized (this) {
            finishedSpanItems.clear();
            isStopped = true;
        }
        return CompletableResultCode.ofSuccess();
    }
}
