package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ResponseConsumptionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void test() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);

        assertThatThrownBy(client::get).isInstanceOfSatisfying(WebApplicationException.class, w -> {
            Response response = w.getResponse();
            // the response should have the response code from the api call
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.readEntity(String.class)).isEqualTo("Unauthorized");
            response.close();
        });
    }

    @Path("resource")
    public interface Client {

        @GET
        Response get();
    }

    @Path("resource")
    public static class Resource {

        @GET
        public RestResponse<String> get() throws InterruptedException {
            return RestResponse.status(RestResponse.Status.UNAUTHORIZED, "Unauthorized");
        }
    }
}
