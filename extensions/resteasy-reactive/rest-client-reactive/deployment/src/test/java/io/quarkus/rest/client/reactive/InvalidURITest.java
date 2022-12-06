package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidURITest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldFailOnMissingSchema() {
        Client client = clientWithUri("localhost:8080");

        Assertions.assertThatThrownBy(client::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailOnMissingColon() {
        Client client = clientWithUri("http//localhost:8080");

        Assertions.assertThatThrownBy(client::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailOnStorkUrlWithoutColonAfterScheme() {
        Client client = clientWithUri("stork//localhost:8080");

        Assertions.assertThatThrownBy(client::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailOnStorkUrlWithoutSlashes() {
        Client client = clientWithUri("stork:somethingwrong");

        Assertions.assertThatThrownBy(client::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldWork() {
        Client client = clientWithUri(
                "http://localhost:" + ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class));

        assertThat(client.get()).isEqualTo("bar-of-chocolate");
    }

    private Client clientWithUri(String uri) {
        return RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(Client.class);
    }

    @Path("/foo")
    public interface Client {
        @GET
        String get();
    }

    @Path("/foo")
    public static class Resource {
        @GET
        public String get() {
            return "bar-of-chocolate";
        }
    }

}
