package io.quarkus.micrometer.opentelemetry.deployment.compatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.quarkus.micrometer.opentelemetry.deployment.common.CountedResource;
import io.quarkus.micrometer.opentelemetry.deployment.common.GuardedResult;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.MetricDataFilter;
import io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * Copy of io.quarkus.micrometer.runtime.MicrometerTimedInterceptorTest
 */
public class MicrometerTimedInterceptorTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "in-memory")
            .overrideConfigKey("quarkus.otel.metric.export.interval", "100ms")
            .overrideConfigKey("quarkus.micrometer.binder.mp-metrics.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withApplicationRoot((jar) -> jar
                    .addClass(CountedResource.class)
                    .addClass(TimedResource.class)
                    .addClass(GuardedResult.class)
                    .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class, MetricDataFilter.class)
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider"));

    @Inject
    TimedResource timed;

    @Inject
    InMemoryMetricExporter metricExporter;

    @BeforeEach
    void setUp() {
        metricExporter.reset();
    }

    @Test
    void testTimeMethod() {
        timed.call(false);

        metricExporter.assertCountDataPointsAtLeastOrEqual("call", null, 1);
        assertEquals(1, metricExporter.get("call")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("call.max")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_Failed() {
        assertThrows(NullPointerException.class, () -> timed.call(true));

        Supplier<MetricDataFilter> metricFilterSupplier = () -> metricExporter.get("call")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag");

        metricExporter.assertCountDataPointsAtLeastOrEqual(metricFilterSupplier, 1);
        assertEquals(1, metricFilterSupplier.get()
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("call.max")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_Async() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.asyncCall(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        Supplier<MetricDataFilter> metricFilterSupplier = () -> metricExporter.get("async.call")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag");

        metricExporter.assertCountDataPointsAtLeastOrEqual(metricFilterSupplier, 1);
        assertEquals(1, metricFilterSupplier.get()
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("async.call.max")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_AsyncFailed() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.asyncCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        metricExporter.assertCountDataPointsAtLeastOrEqual("async.call", null, 1);
        assertEquals(1, metricExporter.get("async.call")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag")
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("async.call.max")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_Uni() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.uniCall(guardedResult);
        guardedResult.complete();
        uni.subscribe().asCompletionStage().join();

        metricExporter.assertCountDataPointsAtLeastOrEqual("uni.call", null, 1);
        assertEquals(1, metricExporter.get("uni.call")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("uni.call.max")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_UniFailed() throws InterruptedException {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.uniCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> uni.subscribe().asCompletionStage().join());

        // this needs to be executed inline, otherwise the results will be old.
        Supplier<MetricDataFilter> metricFilterSupplier = () -> metricExporter.get("uni.call")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag");

        metricExporter.assertCountDataPointsAtLeastOrEqual(metricFilterSupplier, 1);
        assertEquals(1, metricFilterSupplier.get()
                .lastReadingDataPoint(HistogramPointData.class).getCount());

        assertThat(metricExporter.get("uni.call.max")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue())
                .isGreaterThan(0);
    }

    @Test
    void testTimeMethod_LongTaskTimer() {
        timed.longCall(false);
        metricExporter.assertCountDataPointsAtLeastOrEqual("longCall.active", null, 1);
        assertEquals(0, metricExporter.get("longCall.active")
                .tag("method", "longCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Failed() {
        assertThrows(NullPointerException.class, () -> timed.longCall(true));

        metricExporter.assertCountDataPointsAtLeastOrEqual("longCall.active", null, 1);
        assertEquals(0, metricExporter.get("longCall.active")
                .tag("method", "longCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Async() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.longAsyncCall(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        metricExporter.assertCountDataPointsAtLeastOrEqual("async.longCall.active", null, 1);
        assertEquals(0, metricExporter.get("async.longCall.active")
                .tag("method", "longAsyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());
    }

    @Test
    void testTimeMethod_LongTaskTimer_AsyncFailed() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.longAsyncCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        metricExporter.assertCountDataPointsAtLeastOrEqual("async.longCall.active", null, 1);
        assertEquals(0, metricExporter.get("async.longCall.active")
                .tag("method", "longAsyncCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Uni() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.longUniCall(guardedResult);
        guardedResult.complete();
        uni.subscribe().asCompletionStage().join();

        metricExporter.assertCountDataPointsAtLeastOrEqual("uni.longCall.active", null, 1);
        assertEquals(0, metricExporter.get("uni.longCall.active")
                .tag("method", "longUniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());
    }

    @Test
    void testTimeMethod_LongTaskTimer_UniFailed() throws InterruptedException {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.longUniCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> uni.subscribe().asCompletionStage().join());

        // Was "uni.longCall" Now is "uni.longCall.active" and "uni.longCall.duration"
        // Metric was executed but now there are no active tasks

        metricExporter.assertCountDataPointsAtLeastOrEqual("uni.longCall.active", null, 1);
        assertEquals(0, metricExporter.get("uni.longCall.active")
                .tag("method", "longUniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(LongPointData.class).getValue());

        assertEquals(0, metricExporter.get("uni.longCall.duration")
                .tag("method", "longUniCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("extra", "tag")
                .lastReadingDataPoint(DoublePointData.class).getValue());

    }

    @Test
    void testTimeMethod_repeatable() {
        timed.repeatableCall(false);

        metricExporter.assertCountDataPointsAtLeastOrEqual("alpha", null, 1);

        assertEquals(1, metricExporter.get("alpha")
                .tag("method", "repeatableCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingPointsSize());

        assertEquals(1, metricExporter.get("bravo")
                .tag("method", "repeatableCall")
                .tag("class", "io.quarkus.micrometer.opentelemetry.deployment.common.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag")
                .lastReadingPointsSize());
    }

}
