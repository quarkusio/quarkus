package io.quarkus.rest.client.reactive.stork;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.rest.client.reactive.HelloClient2;
import io.quarkus.rest.client.reactive.HelloResource;
import io.quarkus.test.QuarkusUnitTest;

public class StorkResponseTimeLoadBalancerTest {

    private static final String SLOW_RESPONSE = "hello, I'm a slow server";
    private static WireMockServer server;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient2.class, HelloResource.class))
            .withConfigurationResource("stork-stat-lb.properties");

    @BeforeAll
    public static void setUp() {
        server = new WireMockServer(options().port(8766));
        server.stubFor(WireMock.post("/hello/")
                .willReturn(aResponse().withFixedDelay(1000)
                        .withBody(SLOW_RESPONSE).withStatus(200)));
        server.start();
    }

    @AfterAll
    public static void shutDown() {
        server.shutdown();
    }

    @RestClient
    HelloClient2 client;

    @Test
    void shouldUseFasterService() {
        Set<String> responses = new HashSet<>();
        responses.add(client.echo("Bob"));
        responses.add(client.echo("Bob"));

        assertThat(responses).contains("hello, Bob", SLOW_RESPONSE);

        // after hitting the slow endpoint, we should only use the fast one:
        assertThat(client.echo("Alice")).isEqualTo("hello, Alice");
        assertThat(client.echo("Alice")).isEqualTo("hello, Alice");
        assertThat(client.echo("Alice")).isEqualTo("hello, Alice");
    }

}
