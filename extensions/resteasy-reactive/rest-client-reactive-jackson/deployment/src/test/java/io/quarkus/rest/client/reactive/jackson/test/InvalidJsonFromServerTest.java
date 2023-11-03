package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that when the server responds with data that is not valid JSON, we return an internal server error
 */
public class InvalidJsonFromServerTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(JsonObject.class, JsonClient.class, InvalidJsonEndpoint.class));

    @RestClient
    JsonClient client;

    @Test
    public void test() {
        assertThatThrownBy(() -> client.get())
                .isInstanceOf(ClientWebApplicationException.class)
                .hasMessageContaining("HTTP 200")
                .cause()
                .hasMessageContaining("was expecting double-quote to start field name");
    }

    @Path("/invalid-json")
    @RegisterRestClient(baseUri = "http://localhost:8081")
    public interface JsonClient {

        @Produces(MediaType.APPLICATION_JSON)
        @GET
        JsonObject get();
    }

    static class JsonObject {
        public String name;
    }

    @Path("/invalid-json")
    @Produces(MediaType.APPLICATION_JSON)
    public static class InvalidJsonEndpoint {

        @GET
        public String get() {
            return "{name: test}";
        }
    }
}
