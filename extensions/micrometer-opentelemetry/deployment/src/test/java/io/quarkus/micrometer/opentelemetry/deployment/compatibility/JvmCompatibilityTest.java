package io.quarkus.micrometer.opentelemetry.deployment.compatibility;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.PingPongResource;
import io.quarkus.micrometer.opentelemetry.deployment.common.Util;
import io.quarkus.test.QuarkusUnitTest;

public class JvmCompatibilityTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Util.class,
                                    PingPongResource.class,
                                    PingPongResource.PingPongRestClient.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset("""
                                    quarkus.otel.metrics.exporter=in-memory\n
                                    quarkus.otel.metric.export.interval=300ms\n
                                    quarkus.micrometer.binder-enabled-default=false\n
                                    quarkus.micrometer.binder.jvm=true\n
                                    quarkus.redis.devservices.enabled=false\n
                                    """),
                                    "application.properties"));

    @Inject
    protected InMemoryMetricExporter metricExporter;

    // No need to reset tests for JVM

    @Test
    void testDoubleSum() {
        metricExporter.assertCountDataPointsAtLeastOrEqual("jvm.threads.started", null, 1);
        final List<MetricData> metricDataList = metricExporter.getFinishedMetricItems("jvm.threads.started", null);

        metricDataList.forEach(System.out::println);

        final MetricData metricData = metricDataList.stream()
                .max(Comparator.comparingInt(data -> data.getData().getPoints().size()))
                .get();

        assertThat(metricData.getInstrumentationScopeInfo().getName())
                .isEqualTo("io.opentelemetry.micrometer-1.5");

        assertThat(metricData)
                .hasName("jvm.threads.started")
                .hasDescription("The total number of application threads started in the JVM")
                .hasUnit("threads")
                .hasDoubleSumSatisfying(doubleSumAssert -> doubleSumAssert
                        .isMonotonic()
                        .isCumulative()
                        .hasPointsSatisfying(point -> point
                                .satisfies(actual -> assertThat(actual.getValue()).isGreaterThanOrEqualTo(1.0))
                                .hasAttributesSatisfying(attributes -> attributes.isEmpty())));
    }

    @Test
    void testDoubleGauge() {
        metricExporter.assertCountDataPointsAtLeastOrEqual("jvm.classes.loaded", null, 1);
        final List<MetricData> metricDataList = metricExporter.getFinishedMetricItems("jvm.classes.loaded", null);

        metricDataList.forEach(System.out::println);

        final MetricData metricData = metricDataList.stream()
                .max(Comparator.comparingInt(data -> data.getData().getPoints().size()))
                .get();

        assertThat(metricData.getInstrumentationScopeInfo().getName())
                .isEqualTo("io.opentelemetry.micrometer-1.5");

        assertThat(metricData)
                .hasName("jvm.classes.loaded")
                .hasDescription("The number of classes that are currently loaded in the Java virtual machine")
                .hasUnit("classes")
                .hasDoubleGaugeSatisfying(doubleSumAssert -> doubleSumAssert
                        .hasPointsSatisfying(point -> point
                                .satisfies(actual -> assertThat(actual.getValue()).isGreaterThanOrEqualTo(1.0))
                                .hasAttributesSatisfying(attributes -> attributes.isEmpty())));
    }
}
