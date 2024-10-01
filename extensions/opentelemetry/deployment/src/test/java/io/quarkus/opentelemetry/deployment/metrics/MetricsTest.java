package io.quarkus.opentelemetry.deployment.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
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
    protected Meter meter;
    @Inject
    protected InMemoryMetricExporter metricExporter;

    protected static String mapToString(Map<AttributeKey<?>, ?> map) {
        return (String) map.keySet().stream()
                .map(key -> "" + key.getKey() + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @BeforeEach
    void setUp() {
        metricExporter.reset();
    }

    @Test
    void asyncDoubleCounter() {
        final String counterName = "testAsyncDoubleCounter";
        final String counterDescription = "Testing double counter";
        final String counterUnit = "Metric Tonnes";
        assertNotNull(
                meter.counterBuilder(counterName)
                        .ofDoubles()
                        .setDescription(counterDescription)
                        .setUnit(counterUnit)
                        .buildWithCallback(measurement -> {
                            measurement.record(1, Attributes.empty());
                        }));

        metricExporter.assertCountAtLeast(counterName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(counterName, null).get(0);

        assertEquals(metric.getName(), counterName);
        assertEquals(metric.getDescription(), counterDescription);
        assertEquals(metric.getUnit(), counterUnit);

        assertEquals(metric.getDoubleSumData()
                .getPoints()
                .stream()
                .findFirst()
                .get()
                .getValue(), 1);
    }

    @Test
    void asyncLongCounter() {
        final String counterName = "testAsyncLongCounter";
        final String counterDescription = "Testing Async long counter";
        final String counterUnit = "Metric Tonnes";
        assertNotNull(
                meter.counterBuilder(counterName)
                        .setDescription(counterDescription)
                        .setUnit(counterUnit)
                        .buildWithCallback(measurement -> {
                            measurement.record(1, Attributes.empty());
                        }));

        metricExporter.assertCountAtLeast(counterName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(counterName, null).get(0);

        assertEquals(metric.getName(), counterName);
        assertEquals(metric.getDescription(), counterDescription);
        assertEquals(metric.getUnit(), counterUnit);

        assertEquals(metric.getLongSumData()
                .getPoints()
                .stream()
                .findFirst()
                .get()
                .getValue(), 1);
    }

    @Test
    void doubleCounter() {
        final String counterName = "testDoubleCounter";
        final String counterDescription = "Testing double counter";
        final String counterUnit = "Metric Tonnes";

        final double doubleWithAttributes = 20.2;
        final double doubleWithoutAttributes = 10.1;
        DoubleCounter doubleCounter = meter.counterBuilder(counterName)
                .ofDoubles()
                .setDescription(counterDescription)
                .setUnit(counterUnit)
                .build();
        assertNotNull(doubleCounter);

        Map<Double, Attributes> expectedResults = new HashMap<Double, Attributes>();
        expectedResults.put(doubleWithAttributes, Attributes.builder().put("K", "V").build());
        expectedResults.put(doubleWithoutAttributes, Attributes.empty());
        expectedResults.keySet().stream()
                .forEach(key -> doubleCounter.add(key, expectedResults.get(key)));

        metricExporter.assertCountAtLeast(counterName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(counterName, null).get(0);

        assertEquals(metric.getName(), counterName);
        assertEquals(metric.getDescription(), counterDescription);
        assertEquals(metric.getUnit(), counterUnit);

        metric.getDoubleSumData().getPoints().stream()
                .forEach(point -> {
                    assertTrue(expectedResults.containsKey(point.getValue()),
                            "Double" + point.getValue() + " was not an expected result");
                    assertTrue(point.getAttributes().equals(expectedResults.get(point.getValue())),
                            "Attributes were not equal."
                                    + System.lineSeparator() + "Actual values: "
                                    + mapToString(point.getAttributes().asMap())
                                    + System.lineSeparator() + "Expected values: "
                                    + mapToString(expectedResults.get(point.getValue()).asMap()));
                });

    }

    @Test
    void doubleGauge() {
        final String gaugeName = "testDoubleGauge";
        final String gaugeDescription = "Testing double gauge";
        final String gaugeUnit = "ms";
        assertNotNull(
                meter.gaugeBuilder(gaugeName)
                        .setDescription(gaugeDescription)
                        .setUnit("ms")
                        .buildWithCallback(measurement -> {
                            measurement.record(1, Attributes.empty());
                        }));

        metricExporter.assertCountAtLeast(gaugeName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(gaugeName, null).get(0);

        assertEquals(metric.getName(), gaugeName);
        assertEquals(metric.getDescription(), gaugeDescription);
        assertEquals(metric.getUnit(), gaugeUnit);

        assertEquals(metric.getDoubleGaugeData()
                .getPoints()
                .stream()
                .findFirst()
                .get()
                .getValue(), 1);
    }

    @Test
    void doubleHistogram() {
        final String histogramName = "testDoubleHistogram";
        final String histogramDescription = "Testing double histogram";
        final String histogramUnit = "Metric Tonnes";

        final double doubleWithAttributes = 20;
        final double doubleWithoutAttributes = 10;
        DoubleHistogram doubleHistogram = meter.histogramBuilder(histogramName)
                .setDescription(histogramDescription)
                .setUnit(histogramUnit)
                .build();
        assertNotNull(doubleHistogram);

        Map<Double, Attributes> expectedResults = new HashMap<Double, Attributes>();
        expectedResults.put(doubleWithAttributes, Attributes.builder().put("K", "V").build());
        expectedResults.put(doubleWithoutAttributes, Attributes.empty());
        expectedResults.keySet().stream()
                .forEach(key -> doubleHistogram.record(key, expectedResults.get(key)));

        metricExporter.assertCountAtLeast(histogramName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(histogramName, null).get(0);

        assertEquals(metric.getName(), histogramName);
        assertEquals(metric.getDescription(), histogramDescription);
        assertEquals(metric.getUnit(), histogramUnit);

        metric.getHistogramData().getPoints().stream()
                .forEach(point -> {
                    assertTrue(expectedResults.containsKey(point.getSum()),
                            "Double " + point.getSum() + " was not an expected result");
                    assertTrue(point.getAttributes().equals(expectedResults.get(point.getSum())),
                            "Attributes were not equal."
                                    + System.lineSeparator() + "Actual values: "
                                    + mapToString(point.getAttributes().asMap())
                                    + System.lineSeparator() + "Expected values: "
                                    + mapToString(expectedResults.get(point.getSum()).asMap()));
                });
    }

    @Test
    void longCounter() {
        final String counterName = "testLongCounter";
        final String counterDescription = "Testing long counter";
        final String counterUnit = "Metric Tonnes";

        final long longWithAttributes = 24;
        final long longWithoutAttributes = 12;
        LongCounter longCounter = meter.counterBuilder(counterName)
                .setDescription(counterDescription)
                .setUnit(counterUnit)
                .build();
        assertNotNull(longCounter);

        Map<Long, Attributes> expectedResults = new HashMap<Long, Attributes>();
        expectedResults.put(longWithAttributes, Attributes.builder().put("K", "V").build());
        expectedResults.put(longWithoutAttributes, Attributes.empty());
        expectedResults.keySet().stream().forEach(key -> longCounter.add(key, expectedResults.get(key)));

        metricExporter.assertCountAtLeast(counterName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(counterName, null).get(0);

        assertEquals(metric.getName(), counterName);
        assertEquals(metric.getDescription(), counterDescription);
        assertEquals(metric.getUnit(), counterUnit);

        metric.getLongSumData().getPoints().stream()
                .forEach(point -> {
                    assertTrue(expectedResults.containsKey(point.getValue()),
                            "Long" + point.getValue() + " was not an expected result");
                    assertTrue(point.getAttributes().equals(expectedResults.get(point.getValue())),
                            "Attributes were not equal."
                                    + System.lineSeparator() + "Actual values: "
                                    + mapToString(point.getAttributes().asMap())
                                    + System.lineSeparator() + "Expected values: "
                                    + mapToString(expectedResults.get(point.getValue()).asMap()));
                });
    }

    @Test
    void longGauge() {
        final String gaugeName = "testLongGauge";
        final String gaugeDescription = "Testing long gauge";
        final String gaugeUnit = "ms";
        assertNotNull(
                meter.gaugeBuilder(gaugeName)
                        .ofLongs()
                        .setDescription(gaugeDescription)
                        .setUnit("ms")
                        .buildWithCallback(measurement -> {
                            measurement.record(1, Attributes.empty());
                        }));

        metricExporter.assertCountAtLeast(gaugeName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(gaugeName, null).get(0);

        assertEquals(metric.getName(), gaugeName);
        assertEquals(metric.getDescription(), gaugeDescription);
        assertEquals(metric.getUnit(), gaugeUnit);

        assertEquals(metric.getLongGaugeData()
                .getPoints()
                .stream()
                .findFirst()
                .get()
                .getValue(), 1);
    }

    @Test
    void longHistogram() {
        final String histogramName = "testLongHistogram";
        final String histogramDescription = "Testing long histogram";
        final String histogramUnit = "Metric Tonnes";

        final long longWithAttributes = 20;
        final long longWithoutAttributes = 10;
        LongHistogram longHistogram = meter.histogramBuilder(histogramName)
                .ofLongs()
                .setDescription(histogramDescription)
                .setUnit(histogramUnit)
                .build();
        assertNotNull(longHistogram);

        Map<Long, Attributes> expectedResults = new HashMap<Long, Attributes>();
        expectedResults.put(longWithAttributes, Attributes.builder().put("K", "V").build());
        expectedResults.put(longWithoutAttributes, Attributes.empty());

        expectedResults.keySet().stream()
                .forEach(key -> longHistogram.record(key, expectedResults.get(key)));

        metricExporter.assertCountAtLeast(histogramName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(histogramName, null).get(0);

        assertEquals(metric.getName(), histogramName);
        assertEquals(metric.getDescription(), histogramDescription);
        assertEquals(metric.getUnit(), histogramUnit);

        metric.getHistogramData().getPoints().stream()
                .forEach(point -> {
                    assertTrue(expectedResults.containsKey((long) point.getSum()),
                            "Long " + (long) point.getSum() + " was not an expected result");
                    assertTrue(point.getAttributes().equals(expectedResults.get((long) point.getSum())),
                            "Attributes were not equal."
                                    + System.lineSeparator() + "Actual values: "
                                    + mapToString(point.getAttributes().asMap())
                                    + System.lineSeparator() + "Expected values: "
                                    + mapToString(expectedResults.get((long) point.getSum()).asMap()));
                });
    }

    @Test
    void longUpDownCounter() {
        final String counterName = "testLongUpDownCounter";
        final String counterDescription = "Testing long up down counter";
        final String counterUnit = "Metric Tonnes";

        final long longWithAttributes = -20;
        final long longWithoutAttributes = -10;
        LongUpDownCounter longUpDownCounter = meter.upDownCounterBuilder(counterName)
                .setDescription(counterDescription)
                .setUnit(counterUnit)
                .build();
        assertNotNull(longUpDownCounter);

        Map<Long, Attributes> expectedResults = new HashMap<Long, Attributes>();
        expectedResults.put(longWithAttributes, Attributes.builder().put("K", "V").build());
        expectedResults.put(longWithoutAttributes, Attributes.empty());

        expectedResults.keySet().stream()
                .forEach(key -> longUpDownCounter.add(key, expectedResults.get(key)));

        metricExporter.assertCountAtLeast(counterName, null, 1);
        MetricData metric = metricExporter.getFinishedMetricItems(counterName, null).get(0);

        assertEquals(metric.getName(), counterName);
        assertEquals(metric.getDescription(), counterDescription);
        assertEquals(metric.getUnit(), counterUnit);

        metric.getDoubleSumData().getPoints().stream()
                .forEach(point -> {
                    assertTrue(expectedResults.containsKey(point.getValue()),
                            "Long" + point.getValue() + " was not an expected result");
                    assertTrue(point.getAttributes().equals(expectedResults.get(point.getValue())),
                            "Attributes were not equal."
                                    + System.lineSeparator() + "Actual values: "
                                    + mapToString(point.getAttributes().asMap())
                                    + System.lineSeparator() + "Expected values: "
                                    + mapToString(expectedResults.get(point.getValue()).asMap()));
                });

    }
}
