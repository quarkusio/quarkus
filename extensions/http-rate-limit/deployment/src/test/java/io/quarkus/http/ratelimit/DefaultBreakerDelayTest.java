package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Without a customizer the breaker delay is the fixed 5 s, which {@code store-failure-mode=closed} sends as
 * {@code Retry-After}, jittered between one and two delays: a fixed value would bring every refused client back at the
 * instant the breaker probes the store, and that wave could open it again.
 */
public class DefaultBreakerDelayTest {

    private static final String RULE = "quarkus.http.rate-limit.rules.api.";

    // not SwitchableStoreApp, whose nested customizer would come with it
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    SwitchableTestStore.class))
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-failure-mode", "closed")
            .overrideConfigKey(RULE + "paths", "/*")
            .overrideConfigKey(RULE + "phase", "pre-authentication")
            .overrideConfigKey(RULE + "request-key", "ip")
            .overrideRuntimeConfigKey(RULE + "permits", "1")
            .overrideRuntimeConfigKey(RULE + "window", "1m")
            .overrideConfigKey(RULE + "store", "switchable");

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
    void retryAfterIsTheDefaultDelayJittered() {
        SwitchableTestStore.reset();
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        Set<Long> retryAfters = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            HttpResponse<?> response = http.get("/x");
            assertThat(response.statusCode()).isEqualTo(503);
            retryAfters.add(Long.parseLong(response.getHeader("Retry-After")));
        }
        assertThat(retryAfters).allSatisfy(r -> assertThat(r).isBetween(5L, 10L)).hasSizeGreaterThan(1);
    }
}
