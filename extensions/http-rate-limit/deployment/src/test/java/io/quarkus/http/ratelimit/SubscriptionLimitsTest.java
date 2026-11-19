package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * A complete subscription example: the resolver chooses a named limit from the authenticated user's roles, while
 * counters remain per user. Paths and methods are fixed at build time; quotas are supplied when the application starts.
 */
public class SubscriptionLimitsTest {
    private static final String ROOT = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest app = TestHttp.withUsers(new QuarkusExtensionTest())
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, Subscription.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.security.users.embedded.roles.alice", "paid")
            .overrideConfigKey("quarkus.security.users.embedded.users.carol", "carol")
            .overrideConfigKey("quarkus.security.users.embedded.roles.carol", "paid")
            .overrideConfigKey(ROOT + "phase", "post-authentication")
            .overrideConfigKey(ROOT + "request-key", "principal")
            .overrideConfigKey(ROOT + "limit-resolver", "subscription")
            // This rule inherits the shared phase, key and resolver; its paths are fixed at build time.
            .overrideConfigKey(ROOT + "rules.api.paths", "api/*")
            .overrideConfigKey(ROOT + "rules.api.methods", "GET")
            .overrideRuntimeConfigKey(ROOT + "limits.free.permits", "1")
            .overrideRuntimeConfigKey(ROOT + "limits.free.window", "1h")
            .overrideRuntimeConfigKey(ROOT + "limits.paid.permits", "3")
            .overrideRuntimeConfigKey(ROOT + "limits.paid.window", "1h")
            // A static named limit opts this rule out of the inherited resolver.
            .overrideConfigKey(ROOT + "rules.fixed.paths", "fixed/*")
            .overrideRuntimeConfigKey(ROOT + "rules.fixed.limit", "free")
            // Disabling a rule prevents it from counting requests even when paths and a limit are configured.
            .overrideRuntimeConfigKey(ROOT + "rules.disabled.enabled", "false")
            .overrideConfigKey(ROOT + "rules.disabled.paths", "disabled/*")
            .overrideRuntimeConfigKey(ROOT + "rules.disabled.limit", "free");

    @Test
    void freeAndPaidUsersHaveSeparateAllowances() {
        // POST is outside the GET rule, and consumes no allowance.
        for (int i = 0; i < 4; i++) {
            RestAssured.given().auth().preemptive().basic("bob", "bob").post("/api/items").then().statusCode(200);
        }
        assertAllowance("bob", "/api/items", 1);
        assertAllowance("alice", "/api/items", 3);
        assertAllowance("carol", "/api/items", 3);
    }

    @Test
    void explicitStaticLimitOptsOutOfTheInheritedResolver() {
        assertAllowance("alice", "/fixed/items", 1);
        assertAllowance("bob", "/fixed/items", 1);
    }

    @Test
    void disabledRuleDoesNotConsumeItsConfiguredAllowance() {
        for (int i = 0; i < 4; i++) {
            RestAssured.given().auth().preemptive().basic("bob", "bob").get("/disabled/items").then().statusCode(200);
        }
    }

    @Inject
    RateLimits limits;

    @Test
    void configuredLimitsAreAvailableToApplicationCode() {
        assertThat(limits.get("paid").permits()).isEqualTo(3);
        assertThat(limits.get("paid").burst()).isEqualTo(3);
        assertThatThrownBy(() -> limits.get("missing")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    private static void assertAllowance(String user, String path, int permits) {
        for (int i = 0; i < permits; i++) {
            RestAssured.given().auth().preemptive().basic(user, user).get(path).then().statusCode(200);
        }
        RestAssured.given().auth().preemptive().basic(user, user).get(path).then().statusCode(429);
    }

    @Singleton
    @Identifier("subscription")
    public static class Subscription implements RateLimitResolver {
        @Inject
        RateLimits limits;

        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            boolean paid = context.identity().map(identity -> identity.hasRole("paid")).orElse(false);
            return Uni.createFrom().item(limits.get(paid ? "paid" : "free"));
        }
    }
}
