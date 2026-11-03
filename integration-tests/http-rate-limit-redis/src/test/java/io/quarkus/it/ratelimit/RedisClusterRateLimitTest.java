package io.quarkus.it.ratelimit;

import static io.quarkus.it.ratelimit.RedisRateLimitHttpTest.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.runtime.internal.keys.RateLimitKeys;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;

/**
 * The default client in cluster mode: every key is decided by the shard that owns it, and the script is cached there
 * through EVAL (a fresh cluster answers the first EVALSHA on each shard with NOSCRIPT).
 */
@QuarkusTest
@WithTestResource(RedisClusterResource.class)
public class RedisClusterRateLimitTest {

    private static final String PREFIX = "quarkus:http-rate-limit:http-rate-limit-redis:api/pre-authentication:header:";

    @Inject
    RedisDataSource redis;

    @Test
    void keysOnEveryShardAreLimited() {
        List<long[]> shards = new ArrayList<>();
        for (Response range : redis.execute("CLUSTER", "SLOTS")) {
            shards.add(new long[] { range.get(0).toLong(), range.get(1).toLong() });
        }
        assertThat(shards).hasSize(RedisClusterResource.PORTS.length);
        for (long[] shard : shards) {
            String client = clientOn(shard);
            for (int i = 0; i < 3; i++) {
                get("/api/hello", client).then().statusCode(200);
            }
            get("/api/hello", client).then().statusCode(429);
        }
    }

    private String clientOn(long[] slots) {
        while (true) {
            String client = UUID.randomUUID().toString();
            long slot = redis.execute("CLUSTER", "KEYSLOT", PREFIX + RateLimitKeys.sha256(client)).toLong();
            if (slot >= slots[0] && slot <= slots[1]) {
                return client;
            }
        }
    }
}
