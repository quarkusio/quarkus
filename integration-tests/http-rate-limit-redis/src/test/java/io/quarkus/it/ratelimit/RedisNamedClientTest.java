package io.quarkus.it.ratelimit;

import static io.quarkus.it.ratelimit.RedisRateLimitHttpTest.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * {@code quarkus.http.rate-limit.redis.client-name} selects the Redis client of the {@code redis} store. JVM only: it
 * injects the data sources.
 */
@QuarkusTest
@TestProfile(RedisNamedClientTest.NamedClient.class)
public class RedisNamedClientTest {

    @Inject
    RedisDataSource redis;

    @Inject
    @RedisClientName("limits")
    RedisDataSource limits;

    @Test
    void storeUsesTheNamedClient() {
        String client = UUID.randomUUID().toString();
        get("/api/hello", client).then().statusCode(200);

        String key = RedisRateLimitInspectionTest.apiKey(client);
        assertThat(limits.execute("EXISTS", key).toLong()).isEqualTo(1);
        assertThat(redis.execute("EXISTS", key).toLong()).isZero();
    }

    public static class NamedClient implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.rate-limit.redis.client-name", "limits");
        }
    }
}
