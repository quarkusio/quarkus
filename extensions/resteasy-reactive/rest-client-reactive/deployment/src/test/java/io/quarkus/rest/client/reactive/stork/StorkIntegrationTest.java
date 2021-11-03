package io.quarkus.rest.client.reactive.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.HelloClient2;
import io.quarkus.rest.client.reactive.HelloResource;
import io.quarkus.test.QuarkusUnitTest;

public class StorkIntegrationTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloClient2.class, HelloResource.class))
            .withConfigurationResource("stork-application.properties");

    @RestClient
    HelloClient2 client;

    @Test
    void shouldDetermineUrlViaStork() {
        String greeting = RestClientBuilder.newBuilder().baseUri(URI.create("stork://hello-service/hello"))
                .build(HelloClient2.class)
                .echo("black and white bird");
        assertThat(greeting).isEqualTo("hello, black and white bird");
    }

    @Test
    void shouldDetermineUrlViaStorkCDI() {
        String greeting = client.echo("big bird");
        assertThat(greeting).isEqualTo("hello, big bird");
    }

    @Test
    @Timeout(20)
    void shouldFailOnUnknownService() {
        HelloClient2 client2 = RestClientBuilder.newBuilder()
                .baseUri(URI.create("stork://nonexistent-service"))
                .build(HelloClient2.class);
        assertThatThrownBy(() -> client2.echo("foo")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(20)
    void shouldFailForServiceWithoutEndpoints() {
        HelloClient2 client2 = RestClientBuilder.newBuilder()
                .baseUri(URI.create("stork://service-without-endpoints"))
                .build(HelloClient2.class);
        assertThatThrownBy(() -> client2.echo("foo")).isInstanceOf(IllegalArgumentException.class);
    }
}
