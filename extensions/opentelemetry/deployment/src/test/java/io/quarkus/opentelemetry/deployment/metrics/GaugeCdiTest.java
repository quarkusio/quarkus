package io.quarkus.opentelemetry.deployment.metrics;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.MetricData;
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
    public static final String MY_GAUGE_NAME = "my.gauge.name";

    @Inject
    MeterBean meterBean;

    @Inject
    GaugeBean gaugeBean;

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

    /**
     * This is how an invalid classloader looks like:
     * "bean.classloader.name" -> "NULL (SYSTEM CLASSLOADER)"
     * "bean.thread.name" -> "PeriodicMetricReader-1"
     * "resolver.failue" -> "Cannot invoke "java.lang.ClassLoader.getName()" because the return value of
     * "java.lang.Thread.getContextClassLoader()" is null"
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    void validateMetricReaderClassloader() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Map<String, String>> testResultsFuture = gaugeBean.evaluateReaderClassloader();

        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> assertTrue(testResultsFuture.isDone()));
        Map<String, String> testResults = testResultsFuture.get();

        assertNotNull(testResults.get("resolver.classloader.name"), "resolver.classloader.name not found");
        assertTrue(testResults.get("resolver.classloader.name").contains("Quarkus Runtime ClassLoader"),
                "resolver.classloader.name: " + testResults.get("resolver.classloader.name"));

        assertNotNull(testResults.get("bean.classloader.name"), "bean.classloader.name not found");
        assertTrue(testResults.get("bean.classloader.name").contains("Quarkus Runtime ClassLoader"),
                "bean.classloader.name: " + testResults.get("bean.classloader.name"));

        assertNull(testResults.get("resolver.failue"), "resolver.failue: " + testResults.get("resolver.failue"));

        exporter.assertCountAtLeast(MY_GAUGE_NAME, null, 1);
        MetricData metric = exporter.getFinishedMetricItems(MY_GAUGE_NAME, null).get(0);
        assertEquals(42, metric.getLongGaugeData()
                .getPoints()
                .stream()
                .findFirst()
                .get()
                .getValue());
    }

    @ApplicationScoped
    public static class MeterBean {
        @Inject
        Meter meter;

        public Meter getMeter() {
            return meter;
        }
    }

    @ApplicationScoped
    public static class GaugeBean {
        @Inject
        Meter meter;

        @Inject
        ClassLoaderResolver classLoaderResolver;

        public CompletableFuture<Map<String, String>> evaluateReaderClassloader() {
            final Map<String, String> results = new HashMap<>();
            final CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

            meter.gaugeBuilder(MY_GAUGE_NAME)
                    .ofLongs()
                    .setDescription("Gauge metric that demonstrates ContextClassLoader issue")
                    .setUnit("count")
                    .buildWithCallback(measurement -> {
                        // Capture thread and class loader information for verification
                        results.put("bean.thread.name", Thread.currentThread().getName());
                        results.put("bean.classloader.name", getContextClassLoaderName());

                        try {
                            // Try to use the injected dependency (this was failing on in Quarkus 3.30+)
                            results.put("resolver.classloader.name", classLoaderResolver.getName());
                            measurement.record(42);// some value
                        } catch (Exception e) {
                            results.put("resolver.failue", e.getMessage());
                            measurement.record(-1);
                        }
                        future.complete(results);
                    });
            return future;
        }

        private String getContextClassLoaderName() {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader == null) {
                return "NULL (SYSTEM CLASSLOADER)";
            }
            return contextClassLoader.getClass().getName() + " - " + contextClassLoader.getName();
        }
    }

    @ApplicationScoped
    public static class ClassLoaderResolver {
        public String getName() {
            return Thread.currentThread().getContextClassLoader().getName();
        }
    }

}
