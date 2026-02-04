package io.quarkus.it.external.exporter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class ExporterTestBase {

    // Azure Monitor endpoint port, see application.properties
    public static final int HTTP_PORT_NUMBER = 53602;
    protected DefaultExporterTelemetry defaultExporterTelemetry;
    protected AzureExporterTelemetry azureExporterTelemetry;
    private WireMockServer wireMockServer;
    private Traces traces;

    @BeforeEach
    public void startWireMock() {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration().port(HTTP_PORT_NUMBER);
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();
        wireMockServer.stubFor(
                any(urlMatching(".*"))
                        .withPort(HTTP_PORT_NUMBER)
                        .willReturn(aResponse().withStatus(200)));
        defaultExporterTelemetry = new DefaultExporterTelemetry(traces);
        azureExporterTelemetry = new AzureExporterTelemetry(wireMockServer);
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }
}
