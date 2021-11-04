package io.quarkus.rest.client.reactive.ssl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.QuarkusUnitTest;

public class TrustAllTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class))
            .withConfigurationResource("trust-all-test-application.properties");

    WireMockServer server;
    URI baseUri;

    @BeforeEach
    public void setUp() {
        server = new WireMockServer(wireMockConfig().dynamicHttpsPort());
        server.stubFor(WireMock.get("/ssl")
                .willReturn(aResponse().withBody("hello").withStatus(200)));
        server.start();
        baseUri = URI.create("https://localhost:" + server.httpsPort());
    }

    @AfterEach
    public void cleanUp() {
        server.shutdown();
    }

    @Test
    void shouldWorkWithTrustAllAndSelfSignedCert() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(Client.class);
        assertThat(client.get()).isEqualTo("hello");
    }

    @Path("ssl")
    interface Client {
        @GET
        String get();
    }
}
