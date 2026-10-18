package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class PeriodicMetricReaderLogTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider"))
            .overrideConfigKey("quarkus.otel.traces.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "in-memory")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> PeriodicMetricReader.class.getName().equals(record.getLoggerName())
                    && "Exporter failed".equals(record.getMessage()))
            .assertLogRecords(logRecords -> assertThat(logRecords).isEmpty());

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Meter meter;

    @Inject
    InMemoryMetricExporter metricExporter;

    @Test
    void exporterFailureWarningIsSuppressed() {
        CompletableResultCode shutdownResult = metricExporter.shutdown();
        shutdownResult.join(10, SECONDS);
        assertThat(shutdownResult.isSuccess()).isTrue();

        meter.counterBuilder("test.counter").build().add(1);

        CompletableResultCode flushResult = ((OpenTelemetrySdk) openTelemetry).getSdkMeterProvider().forceFlush();
        flushResult.join(10, SECONDS);
    }
}
