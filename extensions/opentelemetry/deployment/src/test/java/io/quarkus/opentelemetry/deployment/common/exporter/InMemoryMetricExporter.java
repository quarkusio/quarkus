package io.quarkus.opentelemetry.deployment.common.exporter;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    private static final List<String> KEY_COMPONENTS = List.of(HTTP_REQUEST_METHOD.getKey(),
            HTTP_ROUTE.getKey(),
            HTTP_RESPONSE_STATUS_CODE.getKey());

    private final Queue<MetricData> finishedMetricItems = new ConcurrentLinkedQueue<>();
    private final AggregationTemporality aggregationTemporality = AggregationTemporality.CUMULATIVE;
    private boolean isStopped = false;

    public static Map<String, String> getPointAttributes(final MetricData metricData, final String path) {
        try {
            return metricData.getData().getPoints().stream()
                    .filter(point -> isPathFound(path, point.getAttributes()))
                    .map(point -> point.getAttributes())
                    .map(attributes1 -> attributes1.asMap())
                    .flatMap(map -> map.entrySet().stream())
                    .collect(toMap(map -> map.getKey().toString(), map -> map.getValue().toString()));
        } catch (Exception e) {
            System.out.println("Error getting point attributes for " + metricData.getName());
            metricData.getData().getPoints().stream()
                    .filter(point -> isPathFound(path, point.getAttributes()))
                    .map(point -> point.getAttributes())
                    .map(attributes1 -> attributes1.asMap())
                    .flatMap(map -> map.entrySet().stream())
                    .forEach(attributeKeyObjectEntry -> System.out
                            .println(attributeKeyObjectEntry.getKey() + " " + attributeKeyObjectEntry.getValue()));
            throw e;
        }
    }

    public static Map<String, PointData> getMostRecentPointsMap(List<MetricData> finishedMetricItems) {
        return finishedMetricItems.stream()
                .flatMap(metricData -> metricData.getData().getPoints().stream())
                // exclude data from /export endpoint
                .filter(InMemoryMetricExporter::notExporterPointData)
                // newer first
                .sorted(Comparator.comparingLong(PointData::getEpochNanos).reversed())
                .collect(toMap(
                        pointData -> pointData.getAttributes().asMap().entrySet().stream()
                                //valid attributes for the resulting map key
                                .filter(entry -> KEY_COMPONENTS.contains(entry.getKey().getKey()))
                                // ensure order
                                .sorted(Comparator.comparing(o -> o.getKey().getKey()))
                                // build key
                                .map(entry -> entry.getKey().getKey() + ":" + entry.getValue().toString())
                                .collect(joining(",")),
                        pointData -> pointData,
                        // most recent points will surface
                        (older, newer) -> newer));
    }

    /*
     * ignore points with /export in the route
     */
    private static boolean notExporterPointData(PointData pointData) {
        return pointData.getAttributes().asMap().entrySet().stream()
                .noneMatch(entry -> entry.getKey().getKey().equals(HTTP_ROUTE.getKey()) &&
                        entry.getValue().toString().contains("/export"));
    }

    private static boolean isPathFound(String path, Attributes attributes) {
        if (path == null) {
            return true;// any match
        }
        Object value = attributes.asMap().get(AttributeKey.stringKey(HTTP_ROUTE.getKey()));
        if (value == null) {
            return false;
        }
        return value.toString().equals(path);
    }

    public void assertCount(final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(count, getFinishedMetricItems().size()));
    }

    public void assertCount(final String name, final String target, final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(count, getFinishedMetricItems(name, target).size()));
    }

    public void assertCountAtLeast(final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(count < getFinishedMetricItems().size()));
    }

    public void assertCountAtLeast(final String name, final String target, final int count) {
        Awaitility.await().atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(count < getFinishedMetricItems(name, target).size()));
    }

    /**
     * Returns a {@code List} of the finished {@code Metric}s, represented by {@code MetricData}.
     *
     * @return a {@code List} of the finished {@code Metric}s.
     */
    public List<MetricData> getFinishedMetricItems() {
        return Collections.unmodifiableList(new ArrayList<>(finishedMetricItems));
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
