package io.quarkus.it.opentelemetry.vertx.exporter.coexistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Enables coexistence so the built-in OTLP exporter is created alongside the custom capturing
 * exporters. This is a build-time flag, so it must be set through a profile to get its own
 * augmentation, keeping the other exporter tests unaffected.
 * <p>
 * The capturing exporters and their telemetry holder are declared as nested static beans: per
 * {@link QuarkusTestProfile}, such beans are only active when this profile is used, so they do not
 * leak into the other exporter tests. That is why no {@code @IfBuildProperty} gating is needed.
 */
public final class CoexistenceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.otel.exporter.otlp.experimental.default-enabled", "true");
    }

    /**
     * In-application holder that records what the custom CDI exporters received, so a test can
     * assert the built-in OTLP exporter and a custom exporter both got the same telemetry.
     */
    @ApplicationScoped
    public static class CapturedTelemetry {

        private final AtomicInteger spanCount = new AtomicInteger();
        private final List<String> metricNames = new CopyOnWriteArrayList<>();
        private final List<String> logBodies = new CopyOnWriteArrayList<>();

        public void addSpans(int count) {
            spanCount.addAndGet(count);
        }

        public void addMetricName(String name) {
            metricNames.add(name);
        }

        public void addLogBody(String body) {
            logBodies.add(body);
        }

        public int getSpanCount() {
            return spanCount.get();
        }

        public List<String> getMetricNames() {
            return metricNames;
        }

        public List<String> getLogBodies() {
            return logBodies;
        }

        public void reset() {
            spanCount.set(0);
            metricNames.clear();
            logBodies.clear();
        }
    }

    /**
     * Custom span exporter that records into {@link CapturedTelemetry}.
     */
    @Singleton
    @Unremovable
    public static class CapturingSpanExporter implements SpanExporter {

        @Inject
        CapturedTelemetry captured;

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            captured.addSpans(spans.size());
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

    /**
     * Custom metric exporter that records into {@link CapturedTelemetry}.
     */
    @Singleton
    @Unremovable
    public static class CapturingMetricExporter implements MetricExporter {

        @Inject
        CapturedTelemetry captured;

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            for (MetricData metric : metrics) {
                captured.addMetricName(metric.getName());
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }

    /**
     * Custom log record exporter that records into {@link CapturedTelemetry}.
     */
    @Singleton
    @Unremovable
    public static class CapturingLogRecordExporter implements LogRecordExporter {

        @Inject
        CapturedTelemetry captured;

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            for (LogRecordData log : logs) {
                Value<?> body = log.getBodyValue();
                captured.addLogBody(body == null ? "" : body.asString());
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
