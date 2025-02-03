package io.quarkus.observability.test;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Duplicated test, similar to observability-lgtm.
 * The difference is dependencies / classpath,
 * where this one has / uses Prometheus Micrometer registry.
 */
@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
@Disabled("Dunno how to scrape from Prometheus container to app, on Linux / CI")
public class LgtmTest {
    protected final Logger log = Logger.getLogger(getClass());

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @Test
    public void testPoke() {
        log.info("Testing Grafana ...");
        String response = RestAssured.get("/api/poke?f=100").body().asString();
        log.info("Response: " + response);
        GrafanaClient client = new GrafanaClient(endpoint, "admin", "admin");

        Awaitility.setDefaultPollInterval(1, TimeUnit.SECONDS); // reduce load on the server. Default is .1s

        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                client::user,
                u -> "admin".equals(u.login));
        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                () -> client.query("xvalue_X"),
                result -> !result.data.result.isEmpty());
        Awaitility.await().atMost(61, TimeUnit.SECONDS).until(
                () -> client.traces("quarkus-integration-test-observability-lgtm-prometheus", 20, 3),
                result -> !result.traces.isEmpty());
    }

}
