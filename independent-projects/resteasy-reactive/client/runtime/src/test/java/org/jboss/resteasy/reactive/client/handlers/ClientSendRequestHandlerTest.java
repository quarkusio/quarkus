package org.jboss.resteasy.reactive.client.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "rest-client", password = "password", formats = Format.JKS))
class ClientSendRequestHandlerTest {
    private static WireMockServer server;
    private static Client client;

    @BeforeAll
    static void start() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (var stream = Files.newInputStream(Path.of("target/certs/rest-client-truststore.jks"))) {
            trustStore.load(stream, "password".toCharArray());
        }
        server = new WireMockServer(options().dynamicPort().dynamicHttpsPort()
                .keystorePath("target/certs/rest-client-keystore.jks")
                .keystorePassword("password").keyManagerPassword("password"));
        server.start();
        server.stubFor(get(urlPathEqualTo("/path"))
                .withQueryParam("query", equalTo("a b&c"))
                .willReturn(aResponse().withBody("hello")));
        client = ClientBuilder.newBuilder().trustStore(trustStore)
                .connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).build();
    }

    @AfterAll
    static void stop() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "http", "https", "HTTP", "HTTPS" })
    void validUriPreservesPathAndQuery(String scheme) {
        int port = "https".equalsIgnoreCase(scheme) ? server.httpsPort() : server.port();
        URI uri = URI.create(scheme + "://localhost:" + port + "/path?query=a%20b%26c");
        assertEquals("hello", client.target(uri).request().get(String.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http//localhost:8080", "/relative", "http:/missing-host", "https:///path",
            "http:opaque", "ftp://localhost/file", "http://bad_host/path" })
    void malformedUriFailsFast(String value) {
        var target = client.target(URI.create(value));
        var failure = assertThrows(ProcessingException.class,
                () -> target.request().get(String.class));
        var cause = assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertEquals("Invalid REST Client URL used: '" + target.getUri() + "'", cause.getMessage());
    }
}
