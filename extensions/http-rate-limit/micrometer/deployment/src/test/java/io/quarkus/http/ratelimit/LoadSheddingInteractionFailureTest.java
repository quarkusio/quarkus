package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.LoadSheddingInteractionApp.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * Every terminal response of the limiter skips the latency sample, not only the 429: here the 503 of a failed store
 * under {@code store-failure-mode=closed}.
 */
public class LoadSheddingInteractionFailureTest {

    private static final String CLOSED = "quarkus.http.rate-limit.rules.closed.";

    @RegisterExtension
    static final QuarkusExtensionTest config = LoadSheddingInteractionApp.adaptive(LoadSheddingInteractionApp.create(
            SwitchableTestStore.class))
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-failure-mode", "closed")
            .overrideConfigKey(CLOSED + "paths", "/closed/*")
            .overrideConfigKey(CLOSED + "phase", "pre-authentication")
            .overrideConfigKey(CLOSED + "store", "switchable")
            .overrideConfigKey(CLOSED + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(CLOSED + "permits", "10")
            .overrideRuntimeConfigKey(CLOSED + "window", "1m");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @AfterEach
    void reset() {
        SwitchableTestStore.reset();
    }

    /**
     * Fails if the 503s were sampled, for the reason given in {@link LoadSheddingInteractionFloodTest}.
     */
    @Test
    void failClosed503SkipsTheSample() throws Exception {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        WebClient client = LoadSheddingInteractionApp.client(vertx);
        try {
            for (int i = 0; i < 50; i++) {
                assertThat(status(vertx, client, url, "/closed/x", "f")).isEqualTo(503);
            }

            LoadSheddingInteractionApp.normalTraffic(vertx, client, url);
            LoadSheddingInteractionApp.assertDetectorUndisturbed(vertx, url);

            // and directly, at the load shedding boundary
            Map<String, String> headers = Map.of("X-Client", "f");
            assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/closed/x",
                    LoadSheddingInteractionApp.probe("closed-503", headers)).statusCode()).isEqualTo(503);
            assertThat(LoadSheddingInteractionApp.recorded("closed-503")).contains(true);
        } finally {
            client.close();
        }
    }
}
