package io.quarkus.micrometer.runtime;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.test.CountedResource;
import io.quarkus.micrometer.test.GuardedResult;
import io.quarkus.micrometer.test.TimedResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class MicrometerTimedInterceptorTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClass(CountedResource.class)
                    .addClass(TimedResource.class)
                    .addClass(GuardedResult.class));

    @Inject
    MeterRegistry registry;

    @Inject
    TimedResource timed;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    void testTimeMethod() {
        timed.call(false);
        Timer timer = registry.get("call")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_Failed() {
        Assertions.assertThrows(NullPointerException.class, () -> timed.call(true));

        Timer timer = registry.get("call")
                .tag("method", "call")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_Async() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.asyncCall(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        Timer timer = registry.get("async.call")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_AsyncFailed() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.asyncCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        Timer timer = registry.get("async.call")
                .tag("method", "asyncCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_Uni() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.uniCall(guardedResult);
        guardedResult.complete();
        uni.subscribe().asCompletionStage().join();

        Timer timer = registry.get("uni.call")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_UniFailed() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.uniCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class,
                () -> uni.subscribe().asCompletionStage().join());

        Timer timer = registry.get("uni.call")
                .tag("method", "uniCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "NullPointerException")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());
    }

    @Test
    void testTimeMethod_LongTaskTimer() {
        timed.longCall(false);
        LongTaskTimer timer = registry.get("longCall")
                .tag("method", "longCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Failed() {
        Assertions.assertThrows(NullPointerException.class, () -> timed.longCall(true));

        LongTaskTimer timer = registry.get("longCall")
                .tag("method", "longCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Async() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.longAsyncCall(guardedResult);
        guardedResult.complete();
        completableFuture.join();

        LongTaskTimer timer = registry.get("async.longCall")
                .tag("method", "longAsyncCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_LongTaskTimer_AsyncFailed() {
        GuardedResult guardedResult = new GuardedResult();
        CompletableFuture<?> completableFuture = timed.longAsyncCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class, () -> completableFuture.join());

        LongTaskTimer timer = registry.get("async.longCall")
                .tag("method", "longAsyncCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_LongTaskTimer_Uni() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.longUniCall(guardedResult);
        guardedResult.complete();
        uni.subscribe().asCompletionStage().join();

        LongTaskTimer timer = registry.get("uni.longCall")
                .tag("method", "longUniCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_LongTaskTimer_UniFailed() {
        GuardedResult guardedResult = new GuardedResult();
        Uni<?> uni = timed.longUniCall(guardedResult);
        guardedResult.complete(new NullPointerException());
        Assertions.assertThrows(java.util.concurrent.CompletionException.class,
                () -> uni.subscribe().asCompletionStage().join());

        LongTaskTimer timer = registry.get("uni.longCall")
                .tag("method", "longUniCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("extra", "tag").longTaskTimer();
        Assertions.assertNotNull(timer);
        Assertions.assertEquals(0, timer.activeTasks());
    }

    @Test
    void testTimeMethod_repeatable() {
        timed.repeatableCall(false);
        Timer alphaTimer = registry.get("alpha")
                .tag("method", "repeatableCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(alphaTimer);
        Assertions.assertEquals(1, alphaTimer.count());
        Timer bravoTimer = registry.get("bravo")
                .tag("method", "repeatableCall")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none")
                .tag("extra", "tag").timer();
        Assertions.assertNotNull(bravoTimer);
        Assertions.assertEquals(1, bravoTimer.count());
    }

    @Test
    void testTimeMethod_emptyServiceLevelObjectives() {
        timed.emptyServiceLevelObjectives();

        Timer timer = registry.get("emptyServiceLevelObjectives")
                .tag("method", "emptyServiceLevelObjectives")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none").timer();

        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());

        HistogramSnapshot snapshot = timer.takeSnapshot();
        Assertions.assertEquals(0, snapshot.histogramCounts().length);
    }

    @Test
    void testTimeMethod_customServiceLevelObjectives() {
        timed.customServiceLevelObjectives();

        Timer timer = registry.get("customServiceLevelObjectives")
                .tag("method", "customServiceLevelObjectives")
                .tag("class", "io.quarkus.micrometer.test.TimedResource")
                .tag("exception", "none").timer();

        Assertions.assertNotNull(timer);
        Assertions.assertEquals(1, timer.count());

        HistogramSnapshot snapshot = timer.takeSnapshot();
        Assertions.assertArrayEquals(new double[] { 1e7, 2.5e7, 5e7, 1e8, 5e8, 1e9 },
                Arrays.stream(snapshot.histogramCounts()).mapToDouble(CountAtBucket::bucket).toArray());
    }

}
