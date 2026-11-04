package io.quarkus.http.ratelimit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.RateLimitRejectionInfo.Reason;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class CustomRefusalWriterTest {
    private static final String ROOT = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest app = rule(rule(rule(rule(rule(new QuarkusExtensionTest(), "quota"), "key"), "limit"),
            "store"), "lease")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, Limits.class, Keys.class,
                    BrokenStore.class, Writer.class, Events.class))
            .overrideConfigKey(ROOT + "rules.key.request-key", "custom:broken")
            .overrideConfigKey(ROOT + "rules.store.store", "broken")
            .overrideConfigKey(ROOT + "rules.lease.store", "broken")
            .overrideRuntimeConfigKey(ROOT + "rules.lease.lease-size", "1")
            .overrideRuntimeConfigKey(ROOT + "store-failure-mode", "closed");

    private static QuarkusExtensionTest rule(QuarkusExtensionTest test, String name) {
        return test.overrideConfigKey(ROOT + "rules." + name + ".paths", "/" + name + "/*")
                .overrideConfigKey(ROOT + "rules." + name + ".phase", "pre-authentication")
                .overrideConfigKey(ROOT + "rules." + name + ".request-key", "header:X-Client")
                .overrideConfigKey(ROOT + "rules." + name + ".limit-resolver", "plans");
    }

    @Inject
    Events events;

    @Test
    void writerAndEventsReceiveEveryRefusalWithTheAvailableContext() {
        events.refusals.clear();
        for (String rule : List.of("quota", "key", "limit", "store", "lease")) {
            String client = UUID.randomUUID().toString();
            if (rule.equals("quota")) {
                given().header("X-Client", client).get("/quota/x").then().statusCode(200);
            }
            Reason reason = switch (rule) {
                case "quota" -> Reason.LIMIT_EXCEEDED;
                case "key" -> Reason.KEY_RESOLUTION_FAILED;
                case "limit" -> Reason.LIMIT_RESOLUTION_FAILED;
                default -> Reason.STORE_UNAVAILABLE;
            };
            int status = rule.equals("quota") ? 429 : 503;
            var response = given().header("X-Client", client).get("/" + rule + "/x");
            assertThat(response.statusCode()).as(rule).isEqualTo(status);
            assertThat(response.body().asString()).isEqualTo("custom refusal");
            assertThat(response.contentType()).startsWith("text/plain");
            assertThat(response.header("X-Reason")).isEqualTo(reason.name());
            assertThat(Long.parseLong(response.header("Retry-After"))).isPositive();
            assertThat(events.refusals).filteredOn(info -> info.rule().equals(rule)).singleElement().satisfies(info -> {
                assertThat(info.reason()).isEqualTo(reason);
                assertThat(info.statusCode()).isEqualTo(status);
                assertThat(info.retryAfterSeconds()).isEqualTo(Long.parseLong(response.header("Retry-After")));
                assertThat(info.key().isPresent()).isEqualTo(!rule.equals("key"));
                if (rule.equals("key") || rule.equals("limit")) {
                    assertThat(info.limit()).isEmpty();
                } else {
                    assertThat(info.limit()).contains(RateLimit.of(1, Duration.ofHours(1)));
                }
            });
        }
    }

    @Test
    void headStillUsesTheCustomWriterWithoutABody() {
        var response = given().head("/key/x");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.header("X-Reason")).isEqualTo(Reason.KEY_RESOLUTION_FAILED.name());
        assertThat(response.body().asString()).isEmpty();
    }

    @Singleton
    public static class Events {
        final List<RateLimitRejectionInfo> refusals = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo info) {
            refusals.add(info);
        }
    }

    @Singleton
    public static class Writer implements RateLimitRejectionWriter {
        @Override
        public void write(RoutingContext context, RateLimitRejectionInfo info) {
            context.response().setStatusCode(info.statusCode()).putHeader("Content-Type", "text/plain")
                    .putHeader("X-Reason", info.reason().name());
            context.response().end(context.request().method() == HttpMethod.HEAD ? "" : "custom refusal");
        }
    }

    @Singleton
    @Identifier("plans")
    public static class Limits implements RateLimitResolver {
        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            return context.rule().equals("limit") ? Uni.createFrom().failure(new IllegalStateException("lookup failed"))
                    : Uni.createFrom().item(RateLimit.of(1, Duration.ofHours(1)));
        }
    }

    @Singleton
    @Identifier("broken")
    public static class Keys implements RateLimitKeyResolver {
        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().failure(new IllegalStateException("missing tenant"));
        }
    }

    @Singleton
    @Identifier("broken")
    public static class BrokenStore implements RateLimitStore {
        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            return Uni.createFrom().failure(new IllegalStateException("store down"));
        }
    }
}
