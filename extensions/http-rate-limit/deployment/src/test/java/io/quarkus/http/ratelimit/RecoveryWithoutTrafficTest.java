package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;

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
 * The store probe closes the breaker without any request traffic.
 */
public class RecoveryWithoutTrafficTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open"), "api", "/*",
            "switchable")
            // one WARN and one INFO per outage, whichever tests ran: the reopen after the failed probe is not a new
            // outage and must not WARN again
            .assertLogRecords(records -> {
                long degraded = SwitchableStoreApp.count(records, Level.WARNING, "is degraded");
                assertThat(degraded).isGreaterThanOrEqualTo(1);
                assertThat(SwitchableStoreApp.count(records, Level.INFO, "recovered")).isEqualTo(degraded);
            });

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
    void probeClosesTheBreakerWithoutRequests() {
        int calls = openBreaker();

        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());
        // no requests from here on: within 2 x delay a probe (cost 0) reaches the store and closes the breaker
        await().atMost(Duration.ofSeconds(4)).until(() -> SwitchableTestStore.probeCalls.get() >= 1);
        assertThat(SwitchableTestStore.calls).hasValue(calls);

        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(250));
    }

    @Test
    void breakerReopensWhenTheFirstProbeFailsAndClosesOnTheNext() {
        openBreaker();

        SwitchableTestStore.behaviour.set(new SwitchableTestStore.FailOnce());
        // the first half-open probe fails and reopens the breaker; the one after the next delay succeeds
        await().atMost(Duration.ofSeconds(8)).until(() -> SwitchableTestStore.probeCalls.get() >= 2);

        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(250));
    }

    /**
     * @return the calls that reached the store; 3 when a success from an earlier test already sits in the breaker's
     *         rolling window, else 4
     */
    private int openBreaker() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 4; i++) {
            assertThat(http.get("/x").statusCode()).isEqualTo(200);
        }
        int calls = SwitchableTestStore.calls.get();
        assertThat(calls).isBetween(3, 4);
        return calls;
    }
}
