package io.quarkus.it.ratelimit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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

    static Response get(String path, String client) {
        return given().header("X-Client", client).get(path);
    }
}
