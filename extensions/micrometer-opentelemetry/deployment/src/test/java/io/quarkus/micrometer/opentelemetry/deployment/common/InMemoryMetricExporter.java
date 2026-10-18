package io.quarkus.micrometer.opentelemetry.deployment.common;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class InMemoryMetricExporter implements MetricExporter {

    private final Queue<MetricData> finishedMetricItems = new ConcurrentLinkedQueue<>();
    private final AggregationTemporality aggregationTemporality = AggregationTemporality.CUMULATIVE;
    private boolean isStopped = false;

    public MetricDataFilter metrics(final String name) {
        return new MetricDataFilter(this, name);
    }

    public MetricDataFilter get(final String name) {
        return new MetricDataFilter(this, name);
    }

    public MetricDataFilter find(final String name) {
        return new MetricDataFilter(this, name);
    }

    /*
     * ignore points with /export in the route
     */
    private static boolean notExporterPointData(PointData pointData) {
        return pointData.getAttributes().asMap().entrySet().stream()
                .noneMatch(entry -> entry.getKey().getKey().equals("uri") &&
                        entry.getValue().toString().contains("/export"));
    }

    private static boolean isPathFound(String path, Attributes attributes) {
        if (path == null) {
            return true;// any match
        }
        Object value = attributes.asMap().get(AttributeKey.stringKey("uri"));
        if (value == null) {
            return false;
        }
        return value.toString().equals(path);
    }

    public MetricData getLastFinishedHistogramItem(String testSummary, int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(count, getFinishedMetricItems(testSummary, null).size()));
        List<MetricData> metricData = getFinishedMetricItems(testSummary, null);
        return metricData.get(metricData.size() - 1);// get last added entry which will be the most recent
    }

    public void assertCountDataPointsAtLeast(final String name, final String target, final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(count < countMaxPoints(name, target)));
    }

    public void assertCountDataPointsAtLeastOrEqual(final String name, final String target, final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(count <= countMaxPoints(name, target)));
    }

    public void assertCountDataPointsAtLeastOrEqual(Supplier<MetricDataFilter> tag, int count) {
        Awaitility.await().atMost(50, SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(count <= tag.get().lastReadingPointsSize()));
    }

    private Integer countMaxPoints(String name, String target) {
        List<MetricData> metricData = getFinishedMetricItems(name, target);
        if (metricData.isEmpty()) {
            return 0;
        }
        int size = metricData.get(metricData.size() - 1).getData().getPoints().size();
        return size;
    }

    /**
     * Returns a {@code List} of the finished {@code Metric}s, represented by {@code MetricData}.
     *
     * @return a {@code List} of the finished {@code Metric}s.
     */
    public List<MetricData> getFinishedMetricItems() {
        return Collections.unmodifiableList(new ArrayList<>(finishedMetricItems));
    }

    public MetricData getFinishedMetricItem(String metricName) {
        List<MetricData> metricData = getFinishedMetricItems(metricName, null);
        if (metricData.isEmpty()) {
            return null;
        }
        return metricData.get(metricData.size() - 1);// get last added entry which will be the most recent
    }

    public List<MetricData> getFinishedMetricItems(final String name, final String target) {
        return Collections.unmodifiableList(new ArrayList<>(
                finishedMetricItems.stream()
                        .filter(metricData -> metricData.getName().equals(name))
                        .filter(metricData -> metricData.getData().getPoints().stream()
                                .anyMatch(point -> isPathFound(target, point.getAttributes())))
                        .collect(Collectors.toList())));
    }

    /**
     * Clears the internal {@code List} of finished {@code Metric}s.
     *
     * <p>
     * Does not reset the state of this exporter if already shutdown.
     */
    public void reset() {
        finishedMetricItems.clear();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return aggregationTemporality;
    }

    /**
     * Exports the collection of {@code Metric}s into the inmemory queue.
     *
     * <p>
     * If this is called after {@code shutdown}, this will return {@code ResultCode.FAILURE}.
     */
    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (isStopped) {
            return CompletableResultCode.ofFailure();
        }
        finishedMetricItems.addAll(metrics);
        return CompletableResultCode.ofSuccess();
    }

    /**
     * The InMemory exporter does not batch metrics, so this method will immediately return with
     * success.
     *
     * @return always Success
     */
    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Clears the internal {@code List} of finished {@code Metric}s.
     *
     * <p>
     * Any subsequent call to export() function on this MetricExporter, will return {@code
     * CompletableResultCode.ofFailure()}
     */
    @Override
    public CompletableResultCode shutdown() {
        isStopped = true;
        finishedMetricItems.clear();
        return CompletableResultCode.ofSuccess();
    }
}
