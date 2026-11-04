package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.ConnectionPoolTooBusyException;
import io.vertx.ext.web.client.HttpResponse;

/**
 * A call refused by the client's connection pool ({@link ConnectionPoolTooBusyException}) is a failed store call for
 * its request, which the failure mode decides, but it does not open the breaker; nor does it close a half-open one.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PoolRefusalTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("closed"), "api", "/*",
            "switchable")
            .assertLogRecords(records -> {
                assertThat(SwitchableStoreApp.count(records, Level.WARNING, "is degraded")).isEqualTo(1);
                assertThat(SwitchableStoreApp.count(records, Level.INFO, "recovered")).isZero();
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
    @Order(1)
    void poolRefusalsFallBackWithoutOpeningTheBreaker() {
        SwitchableTestStore.behaviour.set(poolRefusal());
        for (int i = 0; i < 10; i++) {
            HttpResponse<?> response = http.get("/x");
            // the failure mode decides the request
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(Long.parseLong(response.getHeader("Retry-After"))).isBetween(2L, 4L);
        }
        // the breaker opens after 2 failures in 4 calls, yet every call reached the store
        assertThat(SwitchableTestStore.calls).hasValue(10);
    }

    @Test
    @Order(2)
    void poolRefusalOfTheProbeKeepsTheBreakerOpen() throws InterruptedException {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 6; i++) {
            http.get("/x");
        }
        // a window of successes from the first test opens the breaker after the 2nd to 4th failure
        assertThat(SwitchableTestStore.calls.get()).isBetween(2, 4);

        SwitchableTestStore.behaviour.set(poolRefusal());
        // the half-open probe is refused by the pool, which says nothing about the store: the breaker reopens
        await().atMost(Duration.ofSeconds(4)).until(() -> SwitchableTestStore.probeCalls.get() >= 1);
        int calls = SwitchableTestStore.calls.get();
        for (int i = 0; i < 5; i++) {
            http.get("/x");
            Thread.sleep(100);
        }
        assertThat(SwitchableTestStore.calls).hasValue(calls);
    }

    static SwitchableTestStore.FailWith poolRefusal() {
        return new SwitchableTestStore.FailWith(new ConnectionPoolTooBusyException("Connection pool reached max wait "
                + "queue size of 24"));
    }

    /**
     * Sends 10 requests failing with {@link Ignored} and 10 refused by the pool, which all reach the store, then
     * failing ones until the breaker opens.
     */
    static void assertSkippedThenOpens(Vertx vertx, URL url) {
        SwitchableTestStore.reset();
        TestClient http = new TestClient(vertx, url);
        try {
            SwitchableTestStore.behaviour.set(new SwitchableTestStore.FailWith(new Ignored()));
            send(http, 10);
            SwitchableTestStore.behaviour.set(poolRefusal());
            send(http, 10);
            assertThat(SwitchableTestStore.calls).hasValue(20);

            SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
            send(http, 6);
            // 2 failures in the last 4 calls open the breaker
            assertThat(SwitchableTestStore.calls).hasValue(22);
        } finally {
            http.close();
        }
    }

    private static void send(TestClient http, int requests) {
        for (int i = 0; i < requests; i++) {
            assertThat(http.get("/x").statusCode()).isEqualTo(200);
        }
    }

    public static class Ignored extends RuntimeException {

        public Ignored() {
            super("ignored by the breaker");
        }
    }
}
