package io.quarkus.rest.client.reactive.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class GlobalExceptionMapperDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.test.url", "${test.url}")
            .overrideRuntimeConfigKey("microprofile.rest.client.disable.default.mapper", "true");

    @RestClient
    Client client;

    @TestHTTPResource
    URI baseUri;

    @Test
    void testDeclarativeClient() {
        Response response = client.get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void testProgrammaticClient() {
        OtherClient otherClient = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(OtherClient.class);
        Response response = otherClient.get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Path("/error")
    public static class Resource {
        @GET
        public Response returnError() {
            return Response.status(404).build();
        }
    }

    @Path("/error")
    @RegisterRestClient(configKey = "test")
    public interface Client {
        @GET
        Response get();
    }

    @Path("/error")
    public interface OtherClient {
        @GET
        Response get();
    }
}
