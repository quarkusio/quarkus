package io.quarkus.http.ratelimit;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class ConditionalComponentsTest {
    private static final String ROOT = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, Defaults.class, Selected.class))
            .overrideConfigKey(ROOT + "rules.api.paths", "/*")
            .overrideRuntimeConfigKey("tenant.resolver", "selected")
            .overrideConfigKey(ROOT + "phase", "pre-authentication")
            .overrideConfigKey(ROOT + "request-key", "custom:tenant")
            .overrideConfigKey(ROOT + "limit-resolver", "plan")
            .overrideConfigKey(ROOT + "store", "shared");

    @Test
    void runtimeLookupConditionsAreNotRejectedAtBuildTime() {
        given().get("/x").then().statusCode(200);
    }

    @Singleton
    public static class Defaults {
        @LookupUnlessProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("tenant")
        RateLimitKeyResolver key() {
            return ctx -> Uni.createFrom().failure(new AssertionError("default key selected"));
        }

        @LookupUnlessProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("plan")
        RateLimitResolver limit() {
            return ctx -> Uni.createFrom().failure(new AssertionError("default limit selected"));
        }

        @LookupUnlessProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("shared")
        RateLimitStore store() {
            return (key, cost, policy) -> Uni.createFrom().failure(new AssertionError("default store selected"));
        }
    }

    @Singleton
    public static class Selected {
        @LookupIfProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("tenant")
        RateLimitKeyResolver key() {
            return ctx -> Uni.createFrom().item(Optional.of("tenant"));
        }

        @LookupIfProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("plan")
        RateLimitResolver limit() {
            return ctx -> Uni.createFrom().item(RateLimit.of(7, Duration.ofMinutes(1)));
        }

        @LookupIfProperty(name = "tenant.resolver", stringValue = "selected")
        @Produces
        @Identifier("shared")
        RateLimitStore store() {
            return (key, cost, policy) -> key.equals("tenant") && policy.permits() == 7
                    ? Uni.createFrom().item(RateLimitDecision.allowed(0))
                    : Uni.createFrom().failure(new AssertionError("unexpected key or limit"));
        }
    }
}
