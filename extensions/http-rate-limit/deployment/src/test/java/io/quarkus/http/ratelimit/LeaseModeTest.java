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

/**
 * Strict lease mode over the switchable store: 8 permits per minute, lease-size 4. The chunks are sized by demand and
 * the store's tail, the ninth request is refused with the store's wait, and the refusal is cached.
 */
public class LeaseModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.leaseRule(SwitchableStoreApp.create("local"));

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
    void chunksAreSizedByDemandAndTail() {
        for (int i = 1; i <= 8; i++) {
            assertThat(http.get("/x").statusCode()).as("request %d", i)
                    .isEqualTo(200);
        }
        // 1; then min(2, 7 / 2) = 2 serving 2-3; min(4, 5 / 2) = 2 serving 4-5; then 1, 1, 1
        assertThat(SwitchableTestStore.costs).containsExactly(1L, 2L, 2L, 1L, 1L, 1L);

        HttpResponse<Buffer> ninth = http.get("/x");
        assertThat(ninth.statusCode()).isEqualTo(429);
        assertThat(SwitchableTestStore.costs).containsExactly(1L, 2L, 2L, 1L, 1L, 1L, 1L);
        assertThat(Integer.parseInt(ninth.getHeader("Retry-After"))).isBetween(1, 8);

        // the refusal is cached for the store's wait: no new store call
        assertThat(http.get("/x").statusCode()).isEqualTo(429);
        assertThat(SwitchableTestStore.calls).hasValue(7);
    }
}
