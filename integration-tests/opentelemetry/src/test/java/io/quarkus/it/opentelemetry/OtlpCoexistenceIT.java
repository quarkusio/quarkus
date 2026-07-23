package io.quarkus.it.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OtlpCoexistenceIT.OtlpEnabledProfile.class)
public class OtlpCoexistenceIT {

    @Inject
    BeanManager beanManager;

    @Test
    void testCoexistenceSpan() {
        var spanExporters = beanManager.getBeans(SpanExporter.class);
        assertEquals(3, spanExporters.size(), "SpanExporters missing, found: " + spanExporters);
        assertTrue(spanExporters.stream().anyMatch(b -> b.getBeanClass().getName().contains("InMemorySpanExporterProducer")));
        assertTrue(spanExporters.stream().anyMatch(b -> b.getBeanClass().getName().contains("CustomExporterAndProcessor")));
    }

    @Test
    void testCoexistenceMetricExporters() {
        var metricExporters = beanManager.getBeans(MetricExporter.class);
        assertEquals(3, metricExporters.size(), "MetricExporters missing, found: " + metricExporters);
        assertTrue(
                metricExporters.stream().anyMatch(b -> b.getBeanClass().getName().contains("InMemoryMetricExporterProducer")));
        assertTrue(metricExporters.stream().anyMatch(b -> b.getBeanClass().getName().contains("CustomExporterAndProcessor")));
    }

    @Test
    void testCoexistenceLogRecordExporters() {
        var logRecordExporters = beanManager.getBeans(LogRecordExporter.class);
        assertEquals(3, logRecordExporters.size(), "LogRecordExporters missing, found: " + logRecordExporters);
        assertTrue(logRecordExporters.stream()
                .anyMatch(b -> b.getBeanClass().getName().contains("InMemoryLogRecordExporterProducer")));
        assertTrue(
                logRecordExporters.stream().anyMatch(b -> b.getBeanClass().getName().contains("CustomExporterAndProcessor")));
    }

    @ApplicationScoped
    public static class CustomExporterAndProcessor {

        @Produces
        @Singleton
        public SpanExporter mockSpanExporter() {
            return InMemorySpanExporter.create();
        }

        @Produces
        @Singleton
        public MetricExporter mockMetricExporter() {
            return new MetricExporter() {
                @Override
                public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
                    return AggregationTemporality.CUMULATIVE;
                }

                @Override
                public CompletableResultCode export(Collection<MetricData> metrics) {
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
            };
        }

        @Produces
        @Singleton
        public LogRecordExporter mockLogRecordExporter() {
            return new LogRecordExporter() {
                @Override
                public CompletableResultCode export(Collection<LogRecordData> logs) {
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
            };
        }
    }

    public static class OtlpEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.otel.experimental.otlp.default.enable", "true");
        }
    }
}
