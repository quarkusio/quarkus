package io.quarkus.rest.client.reactive.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.UnknownHostException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.Url;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A CDI client whose interface declares an {@code @Url} parameter can be used without a configured base URL: each call
 * has to provide its own URL, and a call that does not is rejected with the usual message.
 */
public class UrlWithoutBaseUrlTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class));

    @RestClient
    Client client;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer testPort;

    @Test
    public void urlParameterIsEnough() {
        assertThat(client.test(String.format("http://localhost:%d", testPort))).isEqualTo("bar");
    }

    @Test
    public void missingUrlIsReported() {
        assertThatThrownBy(() -> client.test(null))
                .hasMessageContaining("Unable to determine the proper baseUrl/baseUri")
                .hasMessageContaining("Client")
                .isNotInstanceOf(UnknownHostException.class);
    }

    @Test
    public void methodWithoutUrlParameterIsReported() {
        assertThatThrownBy(() -> client.noUrl())
                .hasMessageContaining("Unable to determine the proper baseUrl/baseUri");
    }

    @Path("test")
    @RegisterRestClient
    public interface Client {

        @Path("count")
        @GET
        String test(@Url String uri);

        @Path("count")
        @GET
        String noUrl();
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("count")
        public String test() {
            return "bar";
        }
    }
}
