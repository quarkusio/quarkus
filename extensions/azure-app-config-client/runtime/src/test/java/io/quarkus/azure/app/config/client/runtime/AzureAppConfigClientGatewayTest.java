package io.quarkus.azure.app.config.client.runtime;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;

class AzureAppConfigClientGatewayTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final WireMockServer wireMockServer = new WireMockServer(
            options().notifier(new ConsoleNotifier(true)).port(MOCK_SERVER_PORT));

    private final AzureAppConfigClientGateway sut = new DefaultAzureAppConfigClientGateway(configForTesting());

    @BeforeAll
    static void start() {
        wireMockServer.start();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Test
    void testBasicExchange() throws Exception {
        wireMockServer.stubFor(
                WireMock.get("/kv?key=*&api-version=1.0")
                        .withHeader("x-ms-content-sha256", new ContainsPattern("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="))
                        .willReturn(
                                WireMock.okJson(getJsonStringForApplicationAndProfile())));

        final Response response = sut.exchange();

        assertThat(response).isNotNull().satisfies(r -> {
            assertThat(r.getItems()).hasSize(5);
            assertThat(r.getItems().get(0)).satisfies(i -> {
                assertThat(i.getKey()).isEqualTo(".appconfig.featureflag/test");
            });
        });
    }

    private String getJsonStringForApplicationAndProfile() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("/items.json"), Charset.defaultCharset());
    }

    private static AzureAppConfigClientConfig configForTesting() {
        AzureAppConfigClientConfig azureAppConfigClientConfig = new AzureAppConfigClientConfig();
        azureAppConfigClientConfig.url = "http://localhost:" + MOCK_SERVER_PORT;
        azureAppConfigClientConfig.connectionTimeout = Duration.ZERO;
        azureAppConfigClientConfig.readTimeout = Duration.ZERO;
        azureAppConfigClientConfig.credential = "aCredential";
        azureAppConfigClientConfig.secret = "aSecret";
        return azureAppConfigClientConfig;
    }
}
