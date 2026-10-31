package io.quarkus.observability.test;

import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.logging.Logger;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.restassured.RestAssured;

public abstract class LgtmTestHelper {
    protected final Logger log = Logger.getLogger(getClass());

    protected abstract String grafanaEndpoint();

    protected abstract String prometheusEndpoint();

    protected abstract String tempoEndpoint();

    protected void poke(String path) {
        log.info("Testing Grafana ...");
        String response = RestAssured.get(path + "/poke?f=100").body().asString();
        log.info("Response: " + response);
        GrafanaClient client = new GrafanaClient(grafanaEndpoint(), prometheusEndpoint(), tempoEndpoint(), "admin",
                "admin");

        // Why this test is no longer flaky:
        //  - Before the fix, Awaitility.setDefaultPollInterval(5, s) was mutating
        //    Awaitility's GLOBAL static state, which slowed down EVERY other
        //    Awaitility-based test in the same JVM — and in reverse, other tests'
        //    poll-intervals leaked into this one, causing unpredictable timing.
        //  - Now we set pollInterval(2, s) per await chain (local scope only).
        //  - atMost raised from 90s to 120s to give Grafana/Tempo/Prometheus more
        //    breathing room after each dev-mode reload cycle, especially in CI.
        Awaitility.given().ignoreException(UncheckedIOException.class).pollInterval(2, TimeUnit.SECONDS)
                .await().atMost(120, TimeUnit.SECONDS).until(
                        client::user,
                        u -> "admin".equals(u.login));
        Awaitility.given().ignoreException(UncheckedIOException.class).pollInterval(2, TimeUnit.SECONDS)
                .await().atMost(120, TimeUnit.SECONDS).until(
                        () -> client.query("xvalue_X"),
                        result -> !result.data.result.isEmpty());
        Awaitility.given().ignoreException(UncheckedIOException.class).pollInterval(2, TimeUnit.SECONDS)
                .await().atMost(120, TimeUnit.SECONDS).until(
                        () -> client.traces("quarkus-integration-test-observability-lgtm", 20, 3),
                        result -> !result.traces.isEmpty());
    }
}
