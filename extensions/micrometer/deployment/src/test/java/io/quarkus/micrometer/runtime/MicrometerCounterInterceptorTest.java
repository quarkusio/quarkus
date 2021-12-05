package io.quarkus.micrometer.runtime;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.test.CountedResource;
import io.quarkus.micrometer.test.GuardedResult;
import io.quarkus.micrometer.test.TimedResource;
import io.quarkus.test.QuarkusUnitTest;

public class MicrometerCounterInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.mp-metrics.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withApplicationRoot((jar) -> jar
                    .addClass(CountedResource.class)
                    .addClass(TimedResource.class)
                    .addClass(GuardedResult.class));

    @Inject
    MeterRegistry registry;

    @Inject
    CountedResource counted;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    void testCountFailuresOnly_NoMetricsOnSuccess() {
        counted.onlyCountFailures();
        Assertions.assertThrows(MeterNotFoundException.class, () -> registry.get("metric.none").counter());
    }

    @Test
    void testCountAllMetrics_MetricsOnSuccess() {
        counted.countAllInvocations(false);
        Counter counter = registry.get("metric.all")
                .tag("method", "countAllInvocations")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("extra", "tag")
                .tag("exception", "none")
                .tag("result", "success").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }

    @Test
    void testCountAllMetrics_MetricsOnFailure() {
        Assertions.assertThrows(NullPointerException.class, () -> counted.countAllInvocations(true));
        Counter counter = registry.get("metric.all")
                .tag("method", "countAllInvocations")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("extra", "tag")
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
        Assertions.assertNull(counter.getId().getDescription());
    }

    @Test
    void testCountEmptyMetricName_Success() {
        counted.emptyMetricName(false);
        Counter counter = registry.get("method.counted")
                .tag("method", "emptyMetricName")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("exception", "none")
                .tag("result", "success").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
        Assertions.assertEquals("nice description", counter.getId().getDescription());
    }

    @Test
    void testCountEmptyMetricName_Failure() {
        Assertions.assertThrows(NullPointerException.class, () -> counted.emptyMetricName(true));
        Counter counter = registry.get("method.counted")
                .tag("method", "emptyMetricName")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }

    @Test
    void testCountAsyncFailuresOnly_NoMetricsOnSuccess() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = counted.onlyCountAsyncFailures(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        Assertions.assertThrows(MeterNotFoundException.class, () -> registry.get("async.none").counter());
    }

    @Test
    void testCountAsyncAllMetrics_MetricsOnSuccess() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = counted.countAllAsyncInvocations(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        Counter counter = registry.get("async.all")
                .tag("method", "countAllAsyncInvocations")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("extra", "tag")
                .tag("exception", "none")
                .tag("result", "success").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }

    @Test
    void testCountAsyncAllMetrics_MetricsOnFailure() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = counted.countAllAsyncInvocations(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        Counter counter = registry.get("async.all")
                .tag("method", "countAllAsyncInvocations")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("extra", "tag")
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
        Assertions.assertNull(counter.getId().getDescription());
    }

    @Test
    void testCountAsyncEmptyMetricName_Success() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = counted.emptyAsyncMetricName(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        Counter counter = registry.get("method.counted")
                .tag("method", "emptyAsyncMetricName")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("exception", "none")
                .tag("result", "success").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }

    @Test
    void testCountAsyncEmptyMetricName_Failure() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = counted.emptyAsyncMetricName(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        Counter counter = registry.get("method.counted")
                .tag("method", "emptyMetricName")
                .tag("class", "io.quarkus.micrometer.test.CountedResource")
                .tag("exception", "NullPointerException")
                .tag("result", "failure").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }
}
