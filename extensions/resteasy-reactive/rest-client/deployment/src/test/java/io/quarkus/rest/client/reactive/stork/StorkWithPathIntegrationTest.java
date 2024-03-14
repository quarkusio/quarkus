package io.quarkus.rest.client.reactive.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;

public class StorkWithPathIntegrationTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient.class, HelloResource.class))
            .withConfigurationResource("stork-application-with-path.properties");

    @RestClient
    HelloClient client;

    @Test
    void shouldDetermineUrlViaStork() {
        String greeting = RestClientBuilder.newBuilder().baseUri(URI.create("stork://hello-service"))
                .build(HelloClient.class)
                .echo("black and white bird");
        assertThat(greeting).isEqualTo("hello, black and white bird");

        greeting = RestClientBuilder.newBuilder().baseUri(URI.create("stork://hello-service"))
                .build(HelloClient.class)
                .helloWithPathParam("black and white bird");
        assertThat(greeting).isEqualTo("Hello, black and white bird");
    }

    @Test
    void shouldDetermineUrlViaStorkWhenUsingTarget() throws URISyntaxException {
        String greeting = ClientBuilder.newClient().target("stork://hello-service").request().get(String.class);
        assertThat(greeting).isEqualTo("Hello, World!");

        greeting = ClientBuilder.newClient().target(new URI("stork://hello-service")).request().get(String.class);
        assertThat(greeting).isEqualTo("Hello, World!");

        greeting = ClientBuilder.newClient().target(UriBuilder.fromUri("stork://hello-service/")).request()
                .get(String.class);
        assertThat(greeting).isEqualTo("Hello, World!");

        greeting = ClientBuilder.newClient().target("stork://hello-service/").path("big bird").request()
                .get(String.class);
        assertThat(greeting).isEqualTo("Hello, big bird");
    }

    @Test
    void shouldDetermineUrlViaStorkCDI() {
        String greeting = client.echo("big bird");
        assertThat(greeting).isEqualTo("hello, big bird");

        greeting = client.helloWithPathParam("big bird");
        assertThat(greeting).isEqualTo("Hello, big bird");
    }

    @Test
    @Timeout(20)
    void shouldFailOnUnknownService() {
        HelloClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create("stork://nonexistent-service"))
                .build(HelloClient.class);
        assertThatThrownBy(() -> client.echo("foo")).isInstanceOf(NoSuchServiceDefinitionException.class);
    }
}
