package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.metrics.Meter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With metrics enabled and no {@code Clock} bean of its own, an application must stay on the declarative engine:
 * the default {@code Clock} bean is the SDK's default clock.
 */
public class DefaultClockDeclarativeSdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
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
                                    "application.properties"))
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Inject
    Meter meter;
    @Inject
    InMemoryMetricExporter metricExporter;

    @Test
    void declarativeEngineIsSelected() {
        meter.counterBuilder("default-clock-counter").build().add(5);
        metricExporter.assertCountAtLeast("default-clock-counter", null, 1);
    }
}
