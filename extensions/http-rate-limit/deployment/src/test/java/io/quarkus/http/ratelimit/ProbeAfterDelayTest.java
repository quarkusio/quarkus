package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * The recovery probe runs right when the breaker delay (2 s here) ends, not at the next tick of a fixed period, and
 * again one delay after a probe that failed.
 */
public class ProbeAfterDelayTest {

    private static final Duration DELAY = Duration.ofSeconds(2);
    private static final Duration TOLERANCE = Duration.ofMillis(300);

    @RegisterExtension
    // a limit the tests never reach: every request must reach the store, not a cached refusal
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open"), "api", "/*",
            "switchable")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.api.permits", "1000");

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

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    @Test
    void probeRunsWhenTheDelayEnds() {
        // several outages, so that a probe on a fixed period cannot be on time for all of them by chance
        for (int i = 0; i < 3; i++) {
            SwitchableTestStore.reset();
            long opened = openBreaker();
            SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());

            Duration untilProbe = untilProbe(1, opened);
            assertThat(untilProbe).isBetween(DELAY.minusMillis(50), DELAY.plus(TOLERANCE));
            SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(100));
        }
    }

    @Test
    void probeRunsAgainOneDelayAfterAFailedProbe() {
        openBreaker();
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.FailOnce());
        await().atMost(DELAY.multipliedBy(2)).pollInterval(Duration.ofMillis(5))
                .until(() -> SwitchableTestStore.probeCalls.get() >= 1);
        long failedProbe = System.nanoTime();

        Duration untilNextProbe = untilProbe(2, failedProbe);
        assertThat(untilNextProbe).isBetween(DELAY.minusMillis(50), DELAY.plus(TOLERANCE));
        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(100));
    }

    /**
     * @return when the request that opened the breaker was answered
     */
    private long openBreaker() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        int calls;
        do {
            calls = SwitchableTestStore.calls.get();
            assertThat(http.get("/x").statusCode()).isEqualTo(200);
        } while (SwitchableTestStore.calls.get() > calls);
        // the last request did not reach the store: the one before it opened the breaker
        return System.nanoTime();
    }

    private static Duration untilProbe(int probes, long since) {
        await().atMost(DELAY.multipliedBy(2)).pollInterval(Duration.ofMillis(5))
                .until(() -> SwitchableTestStore.probeCalls.get() >= probes);
        return Duration.ofNanos(System.nanoTime() - since);
    }
}
