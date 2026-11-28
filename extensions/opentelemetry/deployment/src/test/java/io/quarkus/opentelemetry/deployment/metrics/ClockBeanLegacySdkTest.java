package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@link Clock} bean that replaces the default one has no declarative configuration equivalent. It must select
 * the legacy engine and be used by the meter provider, as on the autoconfigure path.
 */
public class ClockBeanLegacySdkTest {

    private static final long FIXED_EPOCH_NANOS = 1_000_000_000L;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FixedClockProducer.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    """
                                            quarkus.otel.traces.exporter=none
                                            quarkus.otel.logs.exporter=none
                                            quarkus.otel.metrics.exporter=in-memory
                                            quarkus.otel.metric.export.interval=300ms
                                            quarkus.datasource.devservices.enabled=false
                                            """),
                                    "application.properties"));

    @Inject
    Meter meter;
    @Inject
    InMemoryMetricExporter metricExporter;

    @Test
    void meterProviderUsesTheClockBean() {
        meter.counterBuilder("clock-bean-counter").build().add(5);
        metricExporter.assertCountAtLeast("clock-bean-counter", null, 1);

        List<MetricData> metrics = metricExporter.getFinishedMetricItems("clock-bean-counter", null);
        assertThat(metrics).isNotEmpty();
        assertThat(metrics.get(0).getLongSumData().getPoints())
                .allSatisfy(point -> assertThat(point.getEpochNanos()).isEqualTo(FIXED_EPOCH_NANOS));
    }

    @Singleton
    public static class FixedClockProducer {

        @Produces
        @Singleton
        Clock clock() {
            return new Clock() {
                @Override
                public long now() {
                    return FIXED_EPOCH_NANOS;
                }

                @Override
                public long nanoTime() {
                    return System.nanoTime();
                }
            };
        }
    }
}
