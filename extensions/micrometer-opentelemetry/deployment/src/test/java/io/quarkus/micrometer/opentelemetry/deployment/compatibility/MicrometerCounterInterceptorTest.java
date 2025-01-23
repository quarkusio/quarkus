package io.quarkus.micrometer.opentelemetry.deployment.compatibility;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.aop.MeterTag;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.Util;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Copy of io.quarkus.micrometer.runtime.MicrometerCounterInterceptorTest
 */
public class MicrometerCounterInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Util.class, CountedBean.class, TestValueResolver.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset("""
                                    quarkus.otel.metrics.exporter=in-memory\n
                                    quarkus.otel.metric.export.interval=300ms\n
                                    quarkus.micrometer.binder.http-client.enabled=true\n
                                    quarkus.micrometer.binder.http-server.enabled=true\n
                                    quarkus.redis.devservices.enabled=false\n
                                    """),
                                    "application.properties"));

    @Inject
    CountedBean countedBean;

    @Inject
    InMemoryMetricExporter exporter;

    @BeforeEach
    void setup() {
        exporter.reset();
    }

    @Test
    void testCountAllMetrics() {
        countedBean.countAllInvocations(false);
        Assertions.assertThrows(NullPointerException.class, () -> countedBean.countAllInvocations(true));

        exporter.assertCountDataPointsAtLeastOrEqual("metric.all", null, 2);

        MetricData metricAll = exporter.getFinishedMetricItem("metric.all");
        assertThat(metricAll)
                .isNotNull()
                .hasName("metric.all")
                .hasDescription("")// currently empty
                .hasUnit("")// currently empty
                .hasDoubleSumSatisfying(sum -> sum.hasPointsSatisfying(
                        point -> point
                                .hasValue(1d)
                                .hasAttributes(attributeEntry(
                                        "class",
                                        "io.quarkus.micrometer.opentelemetry.deployment.compatibility.MicrometerCounterInterceptorTest$CountedBean"),
                                        attributeEntry("method", "countAllInvocations"),
                                        attributeEntry("extra", "tag"),
                                        attributeEntry("do_fail", "prefix_false"),
                                        attributeEntry("exception", "none"),
                                        attributeEntry("result", "success")),
                        point -> point
                                .hasValue(1d)
                                .hasAttributes(attributeEntry(
                                        "class",
                                        "io.quarkus.micrometer.opentelemetry.deployment.compatibility.MicrometerCounterInterceptorTest$CountedBean"),
                                        attributeEntry("method", "countAllInvocations"),
                                        attributeEntry("extra", "tag"),
                                        attributeEntry("do_fail", "prefix_true"),
                                        attributeEntry("exception", "NullPointerException"),
                                        attributeEntry("result", "failure"))));
    }

    @ApplicationScoped
    public static class CountedBean {
        @Counted(value = "metric.none", recordFailuresOnly = true)
        public void onlyCountFailures() {
        }

        @Counted(value = "metric.all", extraTags = { "extra", "tag" })
        public void countAllInvocations(@MeterTag(key = "do_fail", resolver = TestValueResolver.class) boolean fail) {
            if (fail) {
                throw new NullPointerException("Failed on purpose");
            }
        }

        @Counted(description = "nice description")
        public void emptyMetricName(@MeterTag boolean fail) {
            if (fail) {
                throw new NullPointerException("Failed on purpose");
            }
        }
    }

    @Singleton
    public static class TestValueResolver implements ValueResolver {
        @Override
        public String resolve(Object parameter) {
            return "prefix_" + parameter;
        }
    }

}
