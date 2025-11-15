package io.quarkus.opentelemetry.deployment.metrics;

import static io.opentelemetry.sdk.metrics.data.MetricDataType.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;

public abstract class BaseJvmMetricsTest {

    @Inject
    protected InMemoryMetricExporter metricExporter;

    void assertMetric(MetricToAssert metric) {
        assertMetric(metric.name(), metric.description(), metric.metricUnit(), metric.metricType());
    }

    void assertMetric(final String metricName,
            final String metricDescription, final String metricUnit,
            final MetricDataType metricType) {

        metricExporter.assertCountAtLeast(metricName, null, 1);

        List<MetricData> finishedMetricItems = metricExporter.getFinishedMetricItems(metricName, null);
        Set<String> scopeNames = finishedMetricItems.stream()
                .map(MetricData::getInstrumentationScopeInfo)
                .map(InstrumentationScopeInfo::getName)
                .collect(Collectors.toSet());

        MetricData foundMetric = finishedMetricItems.size() > 0 ? finishedMetricItems.get(finishedMetricItems.size() - 1)
                : null; //get last

        assertThat(foundMetric).isNotNull();
        assertThat(foundMetric.getName()).isEqualTo(metricName);
        assertThat(foundMetric.getDescription())
                .withFailMessage(metricName + " Expected: " + metricDescription + " found: " + foundMetric.getDescription())
                .isEqualTo(metricDescription);
        assertThat(foundMetric.getType())
                .withFailMessage(metricName + " Expected: " + metricType + " found: " + foundMetric.getType())
                .isEqualTo(metricType);
        assertThat(foundMetric.getUnit())
                .withFailMessage(metricName + " Expected: " + metricUnit + " found: " + foundMetric.getUnit())
                .isEqualTo(metricUnit);

        assertThat(scopeNames.size())
                .withFailMessage(metricName + " found: " + scopeNames)
                .isLessThanOrEqualTo(1);

        if (foundMetric.getName().equals("jvm.cpu.limit")) {
            return; // skip flaky metrics
        }

        // only one of them will be present per test
        foundMetric.getDoubleSumData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage(metricName + ": Double" + point.getValue() + " was not an expected result")
                            .isGreaterThan(0);
                });

        foundMetric.getLongSumData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage(metricName + ": Long" + point.getValue() + " was not an expected result")
                            .isGreaterThanOrEqualTo(0);
                });

        foundMetric.getDoubleGaugeData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.getValue())
                            .withFailMessage(metricName + ": Double" + point.getValue() + " was not an expected result")
                            .isGreaterThanOrEqualTo(0);
                });

        foundMetric.getHistogramData().getPoints().stream()
                .forEach(point -> {
                    assertThat(point.hasMin()).isTrue();
                    assertThat(point.hasMax()).isTrue();
                    assertThat(point.getCounts().size()).isGreaterThan(0);
                });
    }

    record MetricToAssert(String name, String description, String metricUnit, MetricDataType metricType) {
    }
}
