package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * Two rules on one store (and client) share one circuit breaker.
 */
public class SharedBreakerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(
            SwitchableStoreApp.rule(SwitchableStoreApp.create("open"), "a", "/a/*", "switchable"),
            "b", "/b/*", "switchable");

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
    void breakerOpenedByOneRuleProtectsTheOther() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 6; i++) {
            assertThat(http.get("/a/x").statusCode()).isEqualTo(200);
        }
        int calls = SwitchableTestStore.calls.get();
        assertThat(calls).isBetween(4, 5);

        assertThat(http.get("/b/x").statusCode()).isEqualTo(200);
        assertThat(SwitchableTestStore.calls).hasValue(calls);
    }
}
