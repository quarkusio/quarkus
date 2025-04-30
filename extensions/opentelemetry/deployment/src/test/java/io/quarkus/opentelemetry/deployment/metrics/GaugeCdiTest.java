package io.quarkus.opentelemetry.deployment.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class GaugeCdiTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(InMemoryMetricExporter.class.getPackage())
                            .addClasses(TestUtil.class, MeterBean.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.metrics.enabled=true\n" +
                                            "quarkus.datasource.db-kind=h2\n" +
                                            "quarkus.datasource.jdbc.telemetry=true\n" +
                                            "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.metrics.exporter=in-memory\n" +
                                            "quarkus.otel.metric.export.interval=300ms\n" +
                                            "quarkus.otel.bsp.export.timeout=1s\n" +
                                            "quarkus.otel.bsp.schedule.delay=50\n"),
                                    "application.properties"));

    @Inject
    MeterBean meterBean;

    @Inject
    InMemoryMetricExporter exporter;

    @BeforeEach
    void setUp() {
        exporter.reset();
    }

    @Test
    void gauge() throws InterruptedException {
        meterBean.getMeter()
                .gaugeBuilder("jvm.memory.total")
                .setDescription("Reports JVM memory usage.")
                .setUnit("byte")
                .buildWithCallback(
                        result -> result.record(Runtime.getRuntime().totalMemory(), Attributes.empty()));
        exporter.assertCountAtLeast("jvm.memory.total", null, 1);
        assertNotNull(exporter.getFinishedMetricItems("jvm.memory.total", null).get(0));
    }

    @Test
    void meter() {
        assertNotNull(meterBean.getMeter());
    }

    @ApplicationScoped
    public static class MeterBean {
        @Inject
        Meter meter;

        public Meter getMeter() {
            return meter;
        }
    }
}
