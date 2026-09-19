package io.quarkus.rest.client.reactive.url;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A CDI client without any {@code @Url} parameter and without a configured base URL is still rejected when it is
 * first used.
 */
public class NoUrlParamWithoutBaseUrlTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Client.class));

    @RestClient
    Client client;

    @Test
    public void missingBaseUrlIsReported() {
        assertThatThrownBy(() -> client.test())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to determine the proper baseUrl/baseUri");
    }

    @Path("test")
    @RegisterRestClient
    public interface Client {

        @Path("count")
        @GET
        String test();
    }
}
