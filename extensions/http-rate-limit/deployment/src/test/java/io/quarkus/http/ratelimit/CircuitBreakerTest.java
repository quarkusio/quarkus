package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

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

public class CircuitBreakerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open"), "api", "/*",
            "switchable")
            .assertLogRecords(records -> {
                assertThat(SwitchableStoreApp.count(records, Level.WARNING, "is degraded")).isEqualTo(1);
                // the failure mode as it is configured
                assertThat(SwitchableStoreApp.count(records, Level.WARNING, "store-failure-mode=open applies"))
                        .isEqualTo(1);
                assertThat(SwitchableStoreApp.count(records, Level.INFO, "recovered")).isEqualTo(1);
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
    void breakerOpensAfterFailuresAndClosesAfterTheDelay() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 6; i++) {
            assertThat(http.get("/x").statusCode()).isEqualTo(200);
        }
        // the breaker opens on the fourth failure; the calls after that never reach the store
        assertThat(SwitchableTestStore.calls.get()).isBetween(4, 5);

        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());
        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(6), Duration.ofMillis(250));
    }
}
