package io.quarkus.it.ratelimit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

/**
 * Black-box HTTP assertions only: {@link RedisRateLimitIT} runs them against the native executable.
 */
@QuarkusTest
public class RedisRateLimitHttpTest {

    @Test
    void fourthRequestIsRejected() {
        String client = "c1-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            get("/api/hello", client).then().statusCode(200);
        }
        Response rejected = get("/api/hello", client);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.header("Retry-After"))).isBetween(1L, 20L);
    }

    @Test
    void concurrentConsumersShareOneBucket() throws Exception {
        // a cold first call (pool setup, NOSCRIPT then EVAL) can exceed the store timeout and fail closed with a 503:
        // warm up on a throwaway key, whatever its status, before the burst
        get("/api/hello", "warm-up-" + UUID.randomUUID());
        String client = "c2-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                calls.add(() -> get("/api/hello", client).statusCode());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : executor.invokeAll(calls)) {
                statuses.add(future.get());
            }
            assertThat(statuses).filteredOn(s -> s == 200).hasSize(3);
            assertThat(statuses).filteredOn(s -> s == 429).hasSize(17);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void otherClientIsIndependent() {
        String exhausted = "c3-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            get("/api/hello", exhausted).then().statusCode(200);
        }
        get("/api/hello", exhausted).then().statusCode(429);
        get("/api/hello", "c4-" + UUID.randomUUID()).then().statusCode(200);
    }

    @Test
    void namedSubscriptionsKeepSeparateCountersWithAndWithoutLeases() {
        for (String path : List.of("/subscription/hello", "/subscription-lease/hello")) {
            get(path, "warm-up-" + UUID.randomUUID());
            assertSubscription(path, "free-" + UUID.randomUUID(), 2);
            assertSubscription(path, "paid-" + UUID.randomUUID(), 4);
            assertSubscription(path, "paid-" + UUID.randomUUID(), 4);
        }
    }

    @Test
    void unavailableResolversAndStoresUseProblemResponses() {
        for (String path : List.of("/subscription/hello", "/subscription-lease/hello", "/unavailable-key/x",
                "/unavailable-store/x", "/unavailable-lease/x")) {
            Response response = given().accept("application/problem+json")
                    .header("X-Client", "unavailable-" + UUID.randomUUID()).get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(503);
            assertThat(response.contentType()).isEqualTo("application/problem+json");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(503);
            assertThat(response.jsonPath().getString("title")).isEqualTo("Service Unavailable");
            assertThat(Long.parseLong(response.header("Retry-After"))).isPositive();
            given().header("X-Client", "unavailable-" + UUID.randomUUID()).head(path).then().statusCode(503)
                    .body(emptyOrNullString());
        }
    }

    private static void assertSubscription(String path, String client, int permits) {
        for (int i = 0; i < permits; i++) {
            get(path, client).then().statusCode(200);
        }
        get(path, client).then().statusCode(429);
    }

    static Response get(String path, String client) {
        return given().header("X-Client", client).get(path);
    }
}
