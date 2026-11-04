package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * Calls beyond the fixed cap on concurrent store calls are rejected at once, without queueing, and take the failure
 * mode.
 */
public class InFlightCapTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("closed"), "api", "/*",
            "switchable")
            .overrideRuntimeConfigKey(SwitchableStoreApp.STORE + "timeout", "2s");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    private record Result(int status, Duration elapsed) {
    }

    @Test
    void callsBeyondTheLimitAreRejectedImmediately() throws Exception {
        WebClient client = SwitchableStoreApp.wideClient(vertx);
        try {
            // warms up the client connection and the whole rate limit path, so that the timings below are the store's
            assertThat(TestHttp.get(vertx, client, url, "/x", Map.of()).statusCode()).isEqualTo(200);
            SwitchableTestStore.calls.set(0);

            SwitchableTestStore.behaviour.set(new SwitchableTestStore.Delay(Duration.ofMillis(500)));
            long start = System.nanoTime();
            List<CompletableFuture<Result>> responses = new ArrayList<>();
            int total = SwitchableStoreApp.MAX_CONCURRENT_REQUESTS + 4;
            for (int i = 0; i < total; i++) {
                responses.add(TestHttp.getAsync(vertx, client, url, "/x")
                        .thenApply(r -> new Result(r.statusCode(), Duration.ofNanos(System.nanoTime() - start))));
            }
            List<Result> results = new ArrayList<>();
            for (CompletableFuture<Result> response : responses) {
                results.add(response.get(5, TimeUnit.SECONDS));
            }

            List<Result> rejected = results.stream().filter(r -> r.status == 503).toList();
            List<Result> decided = results.stream().filter(r -> r.status != 503).toList();
            assertThat(rejected).hasSize(4);
            assertThat(decided).hasSize(SwitchableStoreApp.MAX_CONCURRENT_REQUESTS);
            // the rejected ones never waited for the store: every one of them answered before any decided one
            Duration lastRejected = rejected.stream().map(Result::elapsed).max(Duration::compareTo).orElseThrow();
            assertThat(decided).allSatisfy(r -> {
                assertThat(r.status).isIn(200, 429);
                assertThat(r.elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(400));
                assertThat(r.elapsed).isGreaterThan(lastRejected);
            });
            // only the admitted calls reached the store
            assertThat(SwitchableTestStore.calls).hasValue(decided.size());
        } finally {
            client.close();
        }
    }
}
