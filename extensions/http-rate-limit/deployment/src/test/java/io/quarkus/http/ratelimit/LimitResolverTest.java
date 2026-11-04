package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * A limit resolver gives keys their limits, for example a higher one for a client on a paid plan. It applies
 * only to the rules that select it with {@code limit-resolver}.
 */
public class LimitResolverTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "other", "/other/*", 1, "1m")
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.phase", "pre-authentication")
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.request-key", "ip")
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.paths", "/api/*")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, PlanResolver.class))
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.limit-resolver", "plans")
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

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

    @Test
    void overrideReplacesTheConfiguredLimitForTheKey() {
        for (int i = 0; i < 10; i++) {
            assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        }
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
    }

    @Test
    void ruleWithoutOverrideKeepsItsConfiguredLimit() {
        assertThat(http.get("/other/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/other/x").statusCode()).isEqualTo(429);
    }

    @Singleton
    @Identifier("plans")
    public static class PlanResolver implements RateLimitResolver {

        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            return Uni.createFrom().item(RateLimit.of(10, Duration.ofMinutes(1)));
        }
    }
}
