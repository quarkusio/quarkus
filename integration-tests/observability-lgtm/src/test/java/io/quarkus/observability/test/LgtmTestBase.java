package io.quarkus.observability.test;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.observability.test.support.GrafanaClient;
import io.restassured.RestAssured;

public abstract class LgtmTestBase {
    private final Logger log = Logger.getLogger(getClass());

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @Test
    public void testTracing() {
        log.info("Testing Grafana ...");
        String response = RestAssured.get("/api/poke?f=100").body().asString();
        log.info("Response: " + response);
        GrafanaClient client = new GrafanaClient(endpoint, "admin", "admin");
        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                client::user,
                u -> "admin".equals(u.login));
        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                () -> client.query("xvalue_X"),
                result -> !result.data.result.isEmpty());
        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                () -> client.traces("quarkus-integration-test-observability-lgtm", 20, 3),
                result -> !result.traces.isEmpty());
    }

}
