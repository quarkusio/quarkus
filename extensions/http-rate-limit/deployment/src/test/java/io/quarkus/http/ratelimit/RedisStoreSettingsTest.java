package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

/**
 * {@code redis.timeout} and {@code redis.failure-mode} override {@code store-timeout} and {@code store-failure-mode} for
 * the store named {@code redis}, and only for it. The Redis client is absent, so a {@link SwitchableTestStore} stands in
 * for the built-in store; {@code api} uses it, {@code other} uses the same store under its own name.
 */
// failureMode first: timeout leaves api over its limit, a refusal that would then be answered without the store
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisStoreSettingsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(
            SwitchableStoreApp.rule(SwitchableStoreApp.create("closed", RedisStandIn.class), "api", "/api/*", "redis"),
            "other", "/other/*", "switchable")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.redis.timeout", "5s")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.redis.failure-mode", "open");

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

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    @Test
    @Order(1)
    void failureMode() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/other/x").statusCode()).isEqualTo(503);
    }

    @Test
    @Order(2)
    void timeout() {
        // above the 200 ms store-timeout, below the 5 s redis.timeout
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Delay(Duration.ofMillis(500)));
        // the store decides: the second request exceeds the limit of 1 per minute
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
        // the call times out, and store-failure-mode=closed applies
        assertThat(http.get("/other/x").statusCode()).isEqualTo(503);
    }

    @Singleton
    public static class RedisStandIn {

        @Produces
        @Singleton
        @Identifier("redis")
        RateLimitStore redis(@Identifier("switchable") RateLimitStore switchable) {
            return switchable;
        }
    }
}
