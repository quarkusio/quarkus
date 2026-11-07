package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
 * An override that fails, never answers or returns an invalid limit leaves the key with the rule's configured limit:
 * the rule still applies, so that a client that can make the override fail, for example by sending keys that miss its
 * cache until it times out, cannot switch the rule off.
 */
public class OverrideFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = rule(rule(rule(new QuarkusExtensionTest(), "failing"), "slow"), "invalid")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    Failing.class, Slow.class, Invalid.class));

    static QuarkusExtensionTest rule(QuarkusExtensionTest test, String name) {
        String prefix = "quarkus.http.rate-limit." + name + ".";
        return test.overrideConfigKey(prefix + "paths", "/" + name + "/*")
                .overrideConfigKey(prefix + "phase", "pre-authentication")
                .overrideConfigKey(prefix + "request-key", "header:X-Client")
                .overrideConfigKey(prefix + "override", name)
                .overrideRuntimeConfigKey(prefix + "permits", "1")
                .overrideRuntimeConfigKey(prefix + "window", "1h");
    }

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
    void failingOverrideKeepsTheConfiguredLimit() {
        assertConfiguredLimit("/failing/x");
    }

    @Test
    void slowOverrideKeepsTheConfiguredLimit() {
        assertConfiguredLimit("/slow/x");
    }

    @Test
    void invalidOverrideLimitKeepsTheConfiguredLimit() {
        assertConfiguredLimit("/invalid/x");
    }

    private void assertConfiguredLimit(String path) {
        Map<String, String> client = Map.of("X-Client", UUID.randomUUID().toString());
        assertThat(http.get(path, client).statusCode()).isEqualTo(200);
        assertThat(http.get(path, client).statusCode()).isEqualTo(429);
    }

    @Singleton
    @Identifier("failing")
    public static class Failing implements RateLimitOverride {

        @Override
        public Uni<Optional<Limit>> override(RateLimitContext context, String key) {
            return Uni.createFrom().failure(new IllegalStateException("boom"));
        }
    }

    @Singleton
    @Identifier("slow")
    public static class Slow implements RateLimitOverride {

        @Override
        public Uni<Optional<Limit>> override(RateLimitContext context, String key) {
            return Uni.createFrom().nothing();
        }
    }

    @Singleton
    @Identifier("invalid")
    public static class Invalid implements RateLimitOverride {

        @Override
        public Uni<Optional<Limit>> override(RateLimitContext context, String key) {
            // meant to block the key, but a limit has at least one permit
            return Uni.createFrom().item(Optional.of(Limit.of(0, Duration.ofMinutes(1))));
        }
    }
}
