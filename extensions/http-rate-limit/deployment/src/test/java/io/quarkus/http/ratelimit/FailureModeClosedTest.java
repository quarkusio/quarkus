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
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class FailureModeClosedTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("closed"), "api",
            "/api/*", "switchable");

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
    void failuresRejectWith503() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        HttpResponse<Buffer> response = http.get("/api/x");
        assertThat(response.statusCode()).isEqualTo(503);
        // the breaker delay, jittered up to twice its value
        assertThat(Long.parseLong(response.getHeader("Retry-After"))).isBetween(2L, 4L);
        assertThat(response.bodyAsString()).isNullOrEmpty();
    }
}
