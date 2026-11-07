package io.quarkus.it.ratelimit;

import static io.quarkus.it.ratelimit.RedisRateLimitHttpTest.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

/**
 * Lease mode over Redis, black-box HTTP assertions only: {@link RedisLeaseIT} runs them against the native executable.
 */
@QuarkusTest
public class RedisLeaseHttpTest {

    @Test
    void seventhRequestIsRejected() {
        String client = "lease-" + UUID.randomUUID();
        for (int i = 0; i < 6; i++) {
            get("/lease/hello", client).then().statusCode(200);
        }
        Response rejected = get("/lease/hello", client);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.header("Retry-After"))).isBetween(1L, 10L);
    }
}
