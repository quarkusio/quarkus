package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.Guard;
import io.vertx.core.Vertx;

/**
 * Guard customizers tune the breaker of a store, in priority order, without taking over the extension's own state
 * tracking.
 */
public class StoreGuardCustomizerTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();
    static final List<CircuitBreakerState> STATES = new CopyOnWriteArrayList<>();

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, TestClient.class,
                    SwitchableTestStore.class, SwitchableStoreApp.class, Loose.class, Tuned.class))
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-timeout", "200ms")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-failure-mode", "open")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> {
                assertThat(SwitchableStoreApp.count(records, Level.WARNING, "is degraded")).isEqualTo(1);
                assertThat(SwitchableStoreApp.count(records, Level.INFO, "recovered")).isEqualTo(1);
            }), "api", "/*", "switchable");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void breakerOpensAfterFourFailuresAndRecoversAfterOneSecond() {
        // the higher priority applies first, so the lower one has the last word
        assertThat(CALLS).containsExactly("loose:switchable", "tuned:switchable");
        SwitchableTestStore.reset();
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 6; i++) {
            assertThat(http.get("/x").statusCode()).isEqualTo(200);
        }
        // the breaker opens on the fourth failure; the calls after that never reach the store
        assertThat(SwitchableTestStore.calls.get()).isEqualTo(4);

        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());
        long start = System.nanoTime();
        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(100));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThan(Duration.ofMillis(500));
        // the customizer's callback runs next to the extension's own
        assertThat(STATES).startsWith(CircuitBreakerState.OPEN).endsWith(CircuitBreakerState.CLOSED);
    }

    @Singleton
    public static class Loose implements RateLimitStoreGuardCustomizer {

        @Override
        public void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker) {
            CALLS.add("loose:" + storeName);
            breaker.requestVolumeThreshold(100).delay(60, ChronoUnit.SECONDS);
        }

        @Override
        public int priority() {
            return 10;
        }
    }

    @Singleton
    public static class Tuned implements RateLimitStoreGuardCustomizer {

        @Override
        public void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker) {
            CALLS.add("tuned:" + storeName);
            breaker.requestVolumeThreshold(4).delay(1, ChronoUnit.SECONDS).onStateChange(STATES::add);
        }
    }
}
