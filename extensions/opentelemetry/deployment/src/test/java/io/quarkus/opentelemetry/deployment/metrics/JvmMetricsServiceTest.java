package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class JvmMetricsServiceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.metrics.enabled=true\n" +
                                            "quarkus.otel.traces.exporter=none\n" +
                                            "quarkus.otel.logs.exporter=none\n" +
                                            "quarkus.otel.metrics.exporter=in-memory\n" +
                                            "quarkus.otel.metric.export.interval=300ms\n"),
                                    "application.properties"));

    @Inject
    protected InMemoryMetricExporter metricExporter;

    // No need to reset between tests. Data is test independent. Will also run faster.

    @Test
    void testClassLoadedMetrics() throws IOException {
        assertMetric("jvm.class.loaded", "Number of classes loaded since JVM start.", "{class}",
                MetricDataType.LONG_SUM);
    }

    @Test
    void testClassUnloadedMetrics() throws IOException {
        assertMetric("jvm.class.unloaded", "Number of classes unloaded since JVM start.",
                "{class}", MetricDataType.LONG_SUM);
    }

    @Test
    void testClassCountMetrics() {
        assertMetric("jvm.class.count", "Number of classes currently loaded.",
                "{class}", MetricDataType.LONG_SUM);
    }

    @Test
    void testCpuTimeMetric() throws IOException {
        assertMetric("jvm.cpu.time", "CPU time used by the process as reported by the JVM.", "s",
                MetricDataType.DOUBLE_SUM);
    }

    @Test
    void testCpuCountMetric() throws IOException {
        assertMetric("jvm.cpu.count",
                "Number of processors available to the Java virtual machine.", "{cpu}",
                MetricDataType.LONG_SUM);
    }

    @Test
    void testCpuRecentUtilizationMetric() throws IOException {
        assertMetric("jvm.cpu.recent_utilization",
                "Recent CPU utilization for the process as reported by the JVM.", "1",
                MetricDataType.DOUBLE_GAUGE);
    }

    @Test
    void testGarbageCollectionCountMetric() {
        System.gc();
        assertMetric("jvm.gc.duration", "Duration of JVM garbage collection actions.", "s",
                MetricDataType.HISTOGRAM);
    }

    @Test
    void testJvmMemoryUsedMetric() throws IOException {
        assertMetric("jvm.memory.used", "Measure of memory used.", "By",
                MetricDataType.LONG_SUM);
    }

    @Test
    void testJvmMemoryCommittedMetric() throws IOException {
        assertMetric("jvm.memory.committed", "Measure of memory committed.", "By",
                MetricDataType.LONG_SUM);
    }

    @Test
    void testMemoryLimitMetric() throws IOException {
        assertMetric("jvm.memory.limit", "Measure of max obtainable memory.", "By",
                MetricDataType.LONG_SUM);
    }

    @Test
    void testMemoryUsedAfterLastGcMetric() throws IOException {
        assertMetric("jvm.memory.used_after_last_gc",
                "Measure of memory used, as measured after the most recent garbage collection event on this pool.",
                "By",
                MetricDataType.LONG_SUM);
    }

    @Disabled("Because of this challenge: https://github.com/eclipse/microprofile-telemetry/issues/241")
    @Test
    void testThreadCountMetric() throws IOException {
        assertMetric("jvm.thread.count", "Number of executing platform threads.", "{thread}",
                MetricDataType.LONG_SUM);
    }

    private void assertMetric(final String metricName,
            final String metricDescription, final String metricUnit,
            final MetricDataType metricType) {

        metricExporter.assertCountAtLeast(metricName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(metricName, null).get(0);

        assertThat(metric).isNotNull();
        assertThat(metric.getName()).isEqualTo(metricName);
        assertThat(metric.getDescription()).isEqualTo(metricDescription);
        assertThat(metric.getType()).isEqualTo(metricType);
        assertThat(metric.getUnit()).isEqualTo(metricUnit);

        // only one of them will be present per test
        metric.getDoubleSumData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage("Double" + point.getValue() + " was not an expected result")
                            .isGreaterThan(0);
                });

        metric.getLongSumData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage("Long" + point.getValue() + " was not an expected result")
                            .isGreaterThanOrEqualTo(0);
                });

        metric.getDoubleGaugeData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage("Double" + point.getValue() + " was not an expected result")
                            .isGreaterThanOrEqualTo(0);
                });

        metric.getHistogramData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.hasMin()).isTrue();
                    assertThat(point.hasMax()).isTrue();
                    assertThat(point.getCounts().size()).isGreaterThan(0);
                });
    }
}
