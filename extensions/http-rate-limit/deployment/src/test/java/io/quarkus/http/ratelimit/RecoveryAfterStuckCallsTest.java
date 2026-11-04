package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Calls that never complete are given up after the store timeout, which releases their concurrency slot and lets a
 * later probe close the breaker; the test never completes the stuck calls itself.
 */
public class RecoveryAfterStuckCallsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open"), "api", "/*",
            "switchable");

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
    void stuckHalfOpenProbeIsAbandonedAndALaterProbeCloses() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 4; i++) {
            http.get("/x");
        }
        // 3 when a success from the other test already sits in the breaker's rolling window
        assertThat(SwitchableTestStore.calls.get()).isBetween(3, 4);

        // the first half-open probe reaches the store and hangs there
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.NeverComplete());
        await().atMost(Duration.ofSeconds(4)).until(() -> SwitchableTestStore.probeCalls.get() >= 1);
        // the inner deadline gives it up and cancels it
        await().atMost(Duration.ofSeconds(2)).until(() -> SwitchableTestStore.cancelled.get() >= 1);

        // the store is restored only now; a later probe closes the breaker
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());
        await().atMost(Duration.ofSeconds(6)).until(() -> SwitchableTestStore.probeCalls.get() >= 2);
        SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(250));
    }

    @Test
    void slotsHeldByStuckCallsAreReleasedForTheProbe() throws Exception {
        WebClient client = SwitchableStoreApp.wideClient(vertx);
        try {
            // as many calls as there are slots hang and hold them all; the two others are rejected at once; after the
            // store timeout the stuck ones fail too, which opens the breaker
            SwitchableTestStore.behaviour.set(new SwitchableTestStore.NeverComplete());
            int slots = SwitchableStoreApp.MAX_CONCURRENT_REQUESTS;
            List<CompletableFuture<HttpResponse<Buffer>>> responses = new ArrayList<>();
            for (int i = 0; i < slots + 2; i++) {
                responses.add(TestHttp.getAsync(vertx, client, url, "/x"));
            }
            for (CompletableFuture<HttpResponse<Buffer>> response : responses) {
                assertThat(response.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            }
            assertThat(SwitchableTestStore.calls).hasValue(slots);
            await().atMost(Duration.ofSeconds(2)).until(() -> SwitchableTestStore.cancelled.get() >= slots);

            SwitchableTestStore.behaviour.set(new SwitchableTestStore.Succeed());
            await().atMost(Duration.ofSeconds(5)).until(() -> SwitchableTestStore.probeCalls.get() >= 1);
            SwitchableStoreApp.awaitReachesTheStore(http, Duration.ofSeconds(3), Duration.ofMillis(250));
        } finally {
            client.close();
        }
    }
}
