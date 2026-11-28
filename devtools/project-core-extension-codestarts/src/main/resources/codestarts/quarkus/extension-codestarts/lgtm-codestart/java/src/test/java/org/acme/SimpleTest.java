package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
public class SimpleTest {
    protected final Logger log = Logger.getLogger(getClass());

    // Access to Grafana (REST) API, etc
    @ConfigProperty(name = "grafana.endpoint")
    Optional<String> grafanaUrl;

    // Useful for all sorts of (custom) OpenTelemetry handling;
    // metrics, tracing, logging, ...
    //
    // Although LGTM extension already automatically sets required properties
    // by default, if it detects that the OpenTelemetry is used, e.g.
    // quarkus.otel.exporter.otlp.endpoint, quarkus.otel.exporter.otlp.protocol
    // Which are the properties used in the OpenTelemetry extension to enable
    // the above mentioned features ootb: metrics, tracing, logging, ...
    @ConfigProperty(name = "otel-collector.url")
    Optional<String> otelUrl;

    @Test
    public void testPoke() throws Exception {
        log.info("Grafana url ... " + grafanaUrl.orElse("not started"));
        log.info("OTel url ... " + otelUrl.orElse("not started"));

        assumeTrue(grafanaUrl.isPresent(),
                "Set quarkus.observability.enabled-in-tests=true to start LGTM Dev Services in tests");

        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(grafanaUrl.get() + "/api/health")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode());
        assertTrue(otelUrl.isPresent());
    }
}
