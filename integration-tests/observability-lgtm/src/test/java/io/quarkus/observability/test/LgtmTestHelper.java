package io.quarkus.observability.test;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.logging.Logger;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.restassured.RestAssured;

public abstract class LgtmTestHelper {
    protected final Logger log = Logger.getLogger(getClass());

    protected abstract String grafanaEndpoint();

    protected void poke(String path) {
        log.info("Testing Grafana ...");
        String response = RestAssured.get(path + "/poke?f=100").body().asString();
        log.info("Response: " + response);
        GrafanaClient client = new GrafanaClient(grafanaEndpoint(), "admin", "admin");
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
