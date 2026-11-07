package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

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
 * An override gives some keys a limit of their own, for example a higher one for a client on a paid plan. It applies
 * only to the rules that select it with {@code override}.
 */
public class LimitOverrideTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(
            TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1m"), "other", "/other/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, PlanOverride.class))
            .overrideConfigKey("quarkus.http.rate-limit.api.override", "plans")
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
    public static class PlanOverride implements RateLimitOverride {

        @Override
        public Uni<Optional<Limit>> override(RateLimitContext context, String key) {
            return Uni.createFrom().item(key.equals("ip:127.0.0.1")
                    ? Optional.of(Limit.of(10, Duration.ofMinutes(1)))
                    : Optional.empty());
        }
    }
}
