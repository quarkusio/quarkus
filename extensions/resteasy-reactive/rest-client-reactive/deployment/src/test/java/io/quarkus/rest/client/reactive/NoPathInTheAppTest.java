package io.quarkus.rest.client.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.QuarkusUnitTest;

public class NoPathInTheAppTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PlaylistService.class));

    private WireMockServer server;

    @BeforeEach
    public void setUp() {
        server = new WireMockServer(8181);
        server.stubFor(WireMock.get("/hello")
                .willReturn(aResponse().withBody("hello, world!").withStatus(200)));
        server.start();
    }

    @AfterEach
    public void shutdown() {
        server.shutdown();
    }

    @Test
    void shouldGet() {
        PlaylistService client = RestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:8181/hello"))
                .build(PlaylistService.class);
        assertThat(client.get()).isEqualTo("hello, world!");
    }
}
