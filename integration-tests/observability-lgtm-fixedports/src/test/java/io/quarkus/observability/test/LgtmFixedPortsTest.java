package io.quarkus.observability.test;

import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
public class LgtmFixedPortsTest {

    protected final Logger log = Logger.getLogger(getClass());

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @Test
    protected void testPoke() {
        log.info("Testing Grafana ...");
        String response = RestAssured.get("/api/poke?f=100").body().asString();
        log.info("Response: " + response);
        GrafanaClient client = new GrafanaClient(endpoint, "admin", "admin");

        Awaitility.setDefaultPollInterval(5, TimeUnit.SECONDS); // reduce load on the server. Default is .1s

        Awaitility.given().ignoreException(UncheckedIOException.class).await().atMost(90, TimeUnit.SECONDS).until(
                client::user,
                u -> "admin".equals(u.login));
        Awaitility.given().ignoreException(UncheckedIOException.class).await().atMost(90, TimeUnit.SECONDS).until(
                () -> client.query("xvalue_X"),
                result -> !result.data.result.isEmpty());
        Awaitility.given().ignoreException(UncheckedIOException.class).await().atMost(90, TimeUnit.SECONDS).until(
                () -> client.traces("quarkus-integration-test-observability-lgtm-fixedports", 20, 3),
                result -> !result.traces.isEmpty());
    }

}
