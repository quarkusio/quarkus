package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

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
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;

public class FailureModeLocalTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("local"), "api", "/*",
            "switchable");

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
    void failuresFallBackToLocalLimiting() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/x").statusCode()).isEqualTo(429);
        assertThat(SwitchableTestStore.calls).hasValue(2);

        // a call that never completes is given up after the store timeout (200 ms) and decided locally
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.NeverComplete());
        long start = System.nanoTime();
        HttpResponse<Buffer> response = http.send(HttpMethod.GET, "/x", Map.of(), null,
                Duration.ofSeconds(5));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        assertThat(response.statusCode()).isEqualTo(429);
    }
}
