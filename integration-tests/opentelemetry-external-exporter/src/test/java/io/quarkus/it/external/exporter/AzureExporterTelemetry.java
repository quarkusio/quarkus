package io.quarkus.it.external.exporter;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class AzureExporterTelemetry {

    private final WireMockServer wireMockServer;

    public AzureExporterTelemetry(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public Callable<Boolean> telemetryDataContainExport() {
        return () -> !getTelemetryHttpRequests().isEmpty();
    }

    public void verifyExportedTraces() {
        assertThat(getTelemetryHttpRequests()
                .stream()
                .map(request -> new String(request.getBody()))
                .anyMatch(body -> body.contains("\"message\":\"opentelemetry-external-exporter-integration-test")
                        && body.contains("(powered by Quarkus ")
                        && body.contains("started in") && body.contains(
                                "{\"LoggerName\":\"io.quarkus.opentelemetry\",\"LoggingLevel\":\"INFO\",\"log.logger.namespace\":\"org.jboss.logging.Logger\"")))
                .as("Should contain OTel log.").isTrue();
    }

    private List<LoggedRequest> getTelemetryHttpRequests() {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")));
    }
}
