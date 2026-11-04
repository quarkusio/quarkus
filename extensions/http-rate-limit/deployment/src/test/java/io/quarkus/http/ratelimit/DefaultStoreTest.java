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
 * A rule without a {@code store} key uses {@code quarkus.http.rate-limit.store}; a rule with one uses its own.
 */
public class DefaultStoreTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(
            SwitchableStoreApp.rule(SwitchableStoreApp.create("local"), "a", "/a/*", null), "b", "/b/*", "local")
            .overrideConfigKey("quarkus.http.rate-limit.store", "switchable");

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
    void rulesWithoutAStoreUseTheDefaultStore() {
        assertThat(http.get("/a/x").statusCode()).isEqualTo(200);
        assertThat(SwitchableTestStore.calls).hasValue(1);

        assertThat(http.get("/b/x").statusCode()).isEqualTo(200);
        assertThat(SwitchableTestStore.calls).hasValue(1);
    }
}
