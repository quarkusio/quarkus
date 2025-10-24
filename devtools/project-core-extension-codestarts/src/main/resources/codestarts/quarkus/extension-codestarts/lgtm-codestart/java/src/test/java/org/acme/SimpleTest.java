package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;;
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
    String grafanaUrl;

    // Useful for all sorts of (custom) OpenTelemetry handling;
    // metrics, tracing, logging, ...
    //
    // Although LGTM extension already automatically sets required properties
    // by default, if it detects that the OpenTelemetry is used, e.g.
    // quarkus.otel.exporter.otlp.endpoint, quarkus.otel.exporter.otlp.protocol
    // Which are the properties used in the OpenTelemetry extension to enable
    // the above mentioned features ootb: metrics, tracing, logging, ...
    @ConfigProperty(name = "otel-collector.url")
    String otelUrl;

    @Test
    public void testPoke() {
        log.info("Grafana url ... " + grafanaUrl);
        log.info("OTel url ... " + otelUrl);
    }
}
