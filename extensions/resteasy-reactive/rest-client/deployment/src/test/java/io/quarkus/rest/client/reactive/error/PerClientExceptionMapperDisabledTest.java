package io.quarkus.rest.client.reactive.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class PerClientExceptionMapperDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.test.url", "${test.url}")
            .overrideRuntimeConfigKey("quarkus.rest-client.test.disable-default-mapper", "true")
            .overrideRuntimeConfigKey("quarkus.rest-client.test2.url", "${test.url}");

    @RestClient
    Client client;

    @RestClient
    Client2 client2;

    @TestHTTPResource
    URI baseUri;

    @Test
    void testDeclarativeClient() {
        Response response = client.get();
        assertThat(response.getStatus()).isEqualTo(404);

        assertThatThrownBy(() -> client2.get()).isInstanceOf(WebApplicationException.class);
    }

    @Test
    void testProgrammaticClient() {
        OtherClient otherClient = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).disableDefaultMapper(true)
                .build(OtherClient.class);
        Response response = otherClient.get();
        assertThat(response.getStatus()).isEqualTo(404);

        OtherClient otherClient2 = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(OtherClient.class);
        assertThatThrownBy(otherClient2::get).isInstanceOf(WebApplicationException.class);
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
    @RegisterRestClient(configKey = "test2")
    public interface Client2 {
        @GET
        Response get();
    }

    @Path("/error")
    public interface OtherClient {
        @GET
        Response get();
    }
}
