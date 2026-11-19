package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.LoadSheddingInteractionApp.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * A flood of 429s must not disturb the overload detector of load shedding. Every 429 is ended without a latency sample;
 * were it sampled, its few microseconds would become the lowest latency the detector compares every request against,
 * so normal requests would look like they had queued behind a thousand others and the limit would shrink.
 * <p>
 * This is its own application, so that the detector starts from a clean state.
 */
public class LoadSheddingInteractionFloodTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.all.";

    @RegisterExtension
    static final QuarkusExtensionTest config = LoadSheddingInteractionApp.adaptive(LoadSheddingInteractionApp.create())
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    /**
     * Fails if the 429s were sampled: the adapting requests of the normal traffic then take a thousand times the
     * baseline, the estimated queue is the whole limit, and the limit drops below its initial value.
     */
    @Test
    void floodOf429sDoesNotTripTheShedder() throws Exception {
        WebClient client = LoadSheddingInteractionApp.client(vertx);
        try {
            // served requests all take as long as the normal traffic below, so that their samples set a baseline
            // that normal traffic matches
            for (int i = 0; i < 10; i++) {
                assertThat(status(vertx, client, url, "/slow", "c")).isEqualTo(200);
            }
            for (int i = 0; i < 200; i++) {
                assertThat(status(vertx, client, url, "/slow", "c")).isEqualTo(429);
            }

            LoadSheddingInteractionApp.normalTraffic(vertx, client, url);
            LoadSheddingInteractionApp.assertDetectorUndisturbed(vertx, url);
        } finally {
            client.close();
        }
    }
}
