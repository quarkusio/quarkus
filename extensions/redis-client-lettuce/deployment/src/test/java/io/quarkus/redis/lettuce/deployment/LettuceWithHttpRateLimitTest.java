package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

/**
 * Verifies that the Redis store of {@code quarkus-http-rate-limit}, which only uses the data source of its client,
 * counts the requests through the Lettuce backend, and that no Vert.x Redis client is created for it.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceWithHttpRateLimitTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-http-rate-limit-redis-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.paths", "/limited/*")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.phase", "pre-authentication")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.request-key", "header:X-Client")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.permits", "2")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.window", "1m")
            .overrideConfigKey("quarkus.http.rate-limit.rules.limited.store", "redis")
            // a store failure answers 503 instead of falling back to local limiting
            .overrideConfigKey("quarkus.http.rate-limit.store-failure-mode", "closed")
            // the first request opens the Redis connection, which takes longer than the default of 100 ms
            .overrideConfigKey("quarkus.http.rate-limit.store-timeout", "5s");

    @Test
    void theStoreCountsThroughLettuceWithoutAVertxClient() {
        assertThat(VertxClientBeans.clientNames()).isEmpty();

        // No route serves the path: a request within the limit is answered 404, one over it 429.
        String client = "lettuce-" + System.nanoTime();
        assertThat(status(client)).isEqualTo(404);
        assertThat(status(client)).isEqualTo(404);
        assertThat(status(client)).isEqualTo(429);
    }

    private static int status(String client) {
        return RestAssured.given().header("X-Client", client).get("/limited/resource").statusCode();
    }
}
