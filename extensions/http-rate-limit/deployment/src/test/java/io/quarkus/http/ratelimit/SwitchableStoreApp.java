package io.quarkus.http.ratelimit;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.faulttolerance.api.Guard;
import io.vertx.core.Vertx;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * The application of the store fault tolerance tests: {@link SwitchableTestStore} as the {@code switchable} store with a
 * 200 ms timeout, and a breaker that opens after 2 failures in 4 calls and stays open for 2 s.
 */
final class SwitchableStoreApp {

    static final String STORE = "quarkus.http.rate-limit.store-";
    /**
     * The fixed cap of the extension on the calls in flight to one store.
     */
    static final int MAX_CONCURRENT_REQUESTS = 256;
    static final String RULE = "quarkus.http.rate-limit.rules.api.";

    private SwitchableStoreApp() {
    }

    /**
     * @param classes further classes of the application
     */
    static QuarkusExtensionTest create(String failureMode, Class<?>... classes) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, TestClient.class,
                        SwitchableTestStore.class, SwitchableStoreApp.class, TunedBreaker.class).addClasses(classes))
                .overrideRuntimeConfigKey(STORE + "timeout", "200ms")
                .overrideRuntimeConfigKey(STORE + "failure-mode", failureMode)
                .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"));
    }

    /**
     * Adds a pre-authentication rule of one permit per minute per IP.
     *
     * @param store the rule's store; {@code null} leaves it to {@code quarkus.http.rate-limit.store}
     */
    static QuarkusExtensionTest rule(QuarkusExtensionTest test, String name, String path, String store) {
        TestHttp.ipRule(test, name, path, 1, "1m");
        if (store != null) {
            test.overrideConfigKey("quarkus.http.rate-limit.rules." + name + ".store", store);
        }
        return test;
    }

    /**
     * Adds the lease-mode rule of the lease tests: 8 permits per minute per IP, lease-size 4, on the switchable store.
     */
    static QuarkusExtensionTest leaseRule(QuarkusExtensionTest test) {
        return test.overrideConfigKey(RULE + "paths", "/*")
                .overrideConfigKey(RULE + "phase", "pre-authentication")
                .overrideConfigKey(RULE + "request-key", "ip")
                .overrideRuntimeConfigKey(RULE + "permits", "8")
                .overrideRuntimeConfigKey(RULE + "burst", "8")
                .overrideRuntimeConfigKey(RULE + "window", "1m")
                .overrideConfigKey(RULE + "store", "switchable")
                .overrideRuntimeConfigKey(RULE + "lease-size", "4");
    }

    /**
     * @return a client that can hold more than {@link #MAX_CONCURRENT_REQUESTS} requests in flight
     */
    static WebClient wideClient(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions(),
                new PoolOptions().setHttp1MaxSize(MAX_CONCURRENT_REQUESTS + 16));
    }

    /**
     * A closed breaker lets a request reach the store again: sends requests until one does.
     */
    static void awaitReachesTheStore(TestClient http, Duration atMost, Duration pollInterval) {
        await().atMost(atMost).pollInterval(pollInterval).until(() -> {
            int before = SwitchableTestStore.calls.get();
            http.get("/x");
            return SwitchableTestStore.calls.get() > before;
        });
    }

    static long count(List<LogRecord> records, Level level, String text) {
        return records.stream()
                .filter(r -> r.getLevel().equals(level) && message(r).contains(text))
                .count();
    }

    /**
     * A breaker that opens after 2 failures in 4 calls and stays open for 2 s.
     */
    @Singleton
    public static class TunedBreaker implements RateLimitStoreGuardCustomizer {

        @Override
        public void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker) {
            breaker.requestVolumeThreshold(4).failureRatio(0.5).delay(2, ChronoUnit.SECONDS);
        }
    }

    private static String message(LogRecord record) {
        Object[] parameters = record.getParameters();
        return parameters == null ? record.getMessage() : String.format(record.getMessage(), parameters);
    }
}
