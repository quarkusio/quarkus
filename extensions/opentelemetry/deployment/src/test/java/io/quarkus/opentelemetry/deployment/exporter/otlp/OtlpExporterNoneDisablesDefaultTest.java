package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that setting an exporter to "none" takes precedence over
 * quarkus.otel.experimental.otlp.default.enable=true — the default exporter
 * must not be created for a signal explicitly disabled via "none", even when
 * coexistence is requested.
 */
public class OtlpExporterNoneDisablesDefaultTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.experimental.otlp.default.enable", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none");

    @Inject
    Instance<SpanExporter> spanExporterInstance;

    @Inject
    Instance<MetricExporter> metricExporterInstance;

    @Inject
    Instance<LogRecordExporter> logRecordExporterInstance;

    @Test
    void tracesExporterNoneWinsOverDefaultEnable() {
        assertTrue(spanExporterInstance.isUnsatisfied(),
                "No SpanExporter bean should exist when exporter=none, even with defaultExporterEnabled=true");
    }

    @Test
    void metricsExporterNoneWinsOverDefaultEnable() {
        assertTrue(metricExporterInstance.isUnsatisfied(),
                "No MetricExporter bean should exist when exporter=none, even with defaultExporterEnabled=true");
    }

    @Test
    void logsExporterNoneWinsOverDefaultEnable() {
        assertTrue(logRecordExporterInstance.isUnsatisfied(),
                "No LogRecordExporter bean should exist when exporter=none, even with defaultExporterEnabled=true");
    }
}
