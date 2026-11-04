package io.quarkus.http.ratelimit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class ResolverUnavailableTest {
    private static final String ROOT = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest app = rule(rule(rule(rule(rule(rule(rule(new QuarkusExtensionTest(), "failed"), "slow"),
            "null"), "fallback"), "bad-key"), "bad-key-null"), "bad-key-failed")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, Limits.class, Keys.class))
            .overrideConfigKey(ROOT + "rules.bad-key.request-key", "custom:broken")
            .overrideConfigKey(ROOT + "rules.bad-key-null.request-key", "custom:broken")
            .overrideConfigKey(ROOT + "rules.bad-key-failed.request-key", "custom:broken")
            .overrideRuntimeConfigKey(ROOT + "key-resolver-timeout", "10ms")
            .overrideRuntimeConfigKey(ROOT + "limit-resolver-timeout", "10ms")
            .overrideRuntimeConfigKey(ROOT + "rules.fallback.fallback-limit", "safe")
            .overrideRuntimeConfigKey(ROOT + "limits.safe.permits", "1")
            .overrideRuntimeConfigKey(ROOT + "limits.safe.window", "1h");

    private static QuarkusExtensionTest rule(QuarkusExtensionTest test, String name) {
        String prefix = ROOT + "rules." + name + ".";
        return test.overrideConfigKey(prefix + "phase", "pre-authentication")
                .overrideConfigKey(prefix + "request-key", "header:X-Client")
                .overrideConfigKey(prefix + "limit-resolver", "limits")
                .overrideConfigKey(prefix + "paths", "/" + name + "/*");
    }

    @Test
    void failedSlowAndNullLimitsReturn503WithoutAFallback() {
        for (String name : new String[] { "failed", "slow", "null", "bad-key", "bad-key-null", "bad-key-failed" }) {
            var response = given().accept("application/problem+json").header("X-Client", UUID.randomUUID().toString())
                    .get("/" + name + "/items");
            assertThat(response.statusCode()).as(name).isEqualTo(503);
            assertThat(response.contentType()).isEqualTo("application/problem+json");
            assertThat(response.jsonPath().getInt("status")).isEqualTo(503);
            assertThat(response.jsonPath().getString("title")).isEqualTo("Service Unavailable");
            assertThat(Long.parseLong(response.header("Retry-After"))).isBetween(1L, 2L);
        }
    }

    @Test
    void explicitFallbackIsEnforced() {
        String client = UUID.randomUUID().toString();
        given().header("X-Client", client).get("/fallback/items").then().statusCode(200);
        given().header("X-Client", client).get("/fallback/items").then().statusCode(429);
    }

    @Singleton
    @Identifier("limits")
    public static class Limits implements RateLimitResolver {
        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            return switch (context.rule()) {
                case "slow" -> Uni.createFrom().nothing();
                case "null" -> Uni.createFrom().nullItem();
                default -> Uni.createFrom().failure(new IllegalStateException("subscription lookup unavailable"));
            };
        }
    }

    @Singleton
    @Identifier("broken")
    public static class Keys implements RateLimitKeyResolver {
        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return switch (context.rule()) {
                case "bad-key-null" -> Uni.createFrom().nullItem();
                case "bad-key-failed" -> Uni.createFrom().failure(new IllegalStateException("tenant unavailable"));
                default -> Uni.createFrom().nothing();
            };
        }
    }
}
