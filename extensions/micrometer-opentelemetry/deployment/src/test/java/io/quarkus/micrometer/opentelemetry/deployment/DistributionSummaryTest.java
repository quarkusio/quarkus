package io.quarkus.micrometer.opentelemetry.deployment;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class DistributionSummaryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ManualHistogramBean.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset("""
                                    quarkus.otel.metrics.enabled=true\n
                                    quarkus.otel.traces.exporter=none\n
                                    quarkus.otel.logs.exporter=none\n
                                    quarkus.otel.metrics.exporter=in-memory\n
                                    quarkus.otel.metric.export.interval=300ms\n
                                    quarkus.micrometer.binder-enabled-default=false\n
                                    quarkus.micrometer.binder.http-client.enabled=true\n
                                    quarkus.micrometer.binder.http-server.enabled=true\n
                                    quarkus.micrometer.binder.http-server.match-patterns=/one=/two\n
                                    quarkus.micrometer.binder.http-server.ignore-patterns=/two\n
                                    quarkus.micrometer.binder.vertx.enabled=true\n
                                    pingpong/mp-rest/url=${test.url}\n
                                    quarkus.redis.devservices.enabled=false\n
                                    """),
                                    "application.properties"));

    @Inject
    ManualHistogramBean manualHistogramBean;

    @Inject
    InMemoryMetricExporter exporter;

    @Test
    void histogramTest() {
        manualHistogramBean.recordHistogram();

        MetricData testSummary = exporter.getLastFinishedHistogramItem("testSummary", 4);
        assertNotNull(testSummary);
        assertThat(testSummary)
                .hasDescription("This is a test distribution summary")
                .hasUnit("things")
                .hasHistogramSatisfying(
                        histogram -> histogram.hasPointsSatisfying(
                                points -> points
                                        .hasSum(555.5)
                                        .hasCount(4)
                                        .hasAttributes(attributeEntry("tag", "value"))));

        MetricData textSummaryMax = exporter.getFinishedMetricItem("testSummary.max");
        assertNotNull(textSummaryMax);
        assertThat(textSummaryMax)
                .hasDescription("This is a test distribution summary")
                .hasDoubleGaugeSatisfying(
                        gauge -> gauge.hasPointsSatisfying(
                                point -> point
                                        .hasValue(500)
                                        .hasAttributes(attributeEntry("tag", "value"))));

        MetricData testSummaryHistogram = exporter.getFinishedMetricItem("testSummary.histogram"); // present when SLOs are set
        assertNotNull(testSummaryHistogram);
        assertThat(testSummaryHistogram)
                .hasDoubleGaugeSatisfying(
                        gauge -> gauge.hasPointsSatisfying(
                                point -> point
                                        .hasValue(1)
                                        .hasAttributes(
                                                attributeEntry("le", "1"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(2)
                                        .hasAttributes(
                                                attributeEntry("le", "10"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(3)
                                        .hasAttributes(
                                                attributeEntry("le", "100"),
                                                attributeEntry("tag", "value")),
                                point -> point
                                        .hasValue(4)
                                        .hasAttributes(
                                                attributeEntry("le", "1000"),
                                                attributeEntry("tag", "value"))));
    }

    @ApplicationScoped
    public static class ManualHistogramBean {
        @Inject
        MeterRegistry registry;

        public void recordHistogram() {
            DistributionSummary summary = DistributionSummary.builder("testSummary")
                    .description("This is a test distribution summary")
                    .baseUnit("things")
                    .tags("tag", "value")
                    .serviceLevelObjectives(1, 10, 100, 1000)
                    .distributionStatisticBufferLength(10)
                    .register(registry);

            summary.record(0.5);
            summary.record(5);
            summary.record(50);
            summary.record(500);
        }
    }
}
