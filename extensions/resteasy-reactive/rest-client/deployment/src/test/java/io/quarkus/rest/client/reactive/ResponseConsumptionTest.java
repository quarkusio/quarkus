package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Random;

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
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class ResponseConsumptionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void testBlockingOk() {
        Client client = createClient();
        Response response200 = client.get200();
        assertThat(response200.getStatus()).isEqualTo(200);
        assertThat(response200.readEntity(byte[].class)).hasSize(Resource.OK_RESPONSE_SIZE);
    }

    @Test
    public void testBlockingError() {
        Client client = createClient();
        assertThatThrownBy(client::get401).isInstanceOfSatisfying(WebApplicationException.class, w -> {
            Response response = w.getResponse();
            // the response should have the response code from the api call
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.readEntity(String.class)).isEqualTo("Unauthorized");
            response.close();
        });
    }

    @Test
    @RunOnVertxContext
    public void testEventLoopOk(UniAsserter asserter) {
        Client client = createClient();

        asserter.assertThat(client::uniGet200, (res) -> {
            assertThat(res.getStatus()).isEqualTo(200);
            assertThat(res.readEntity(byte[].class)).hasSize(Resource.OK_RESPONSE_SIZE);
        });
    }

    @Test
    @RunOnVertxContext
    public void testEventLoopError(UniAsserter asserter) {
        Client client = createClient();

        asserter.assertThat(() -> {
            return client.uniGet401().onFailure(WebApplicationException.class).recoverWithItem(wae -> {
                return ((WebApplicationException) wae).getResponse();
            });
        }, (res) -> {
            assertThat(res.getStatus()).isEqualTo(401);
            assertThat(res.readEntity(String.class)).isEqualTo("Unauthorized");
        });
    }

    private Client createClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);
    }

    @Path("resource")
    public interface Client {

        @GET
        @Path("401")
        Response get401();

        @GET
        @Path("401")
        Uni<Response> uniGet401();

        @GET
        @Path("200")
        Response get200();

        @GET
        @Path("200")
        Uni<Response> uniGet200();
    }

    @Path("resource")
    public static class Resource {

        public static final int OK_RESPONSE_SIZE = 2 * 1024 * 1024;

        @Path("401")
        @GET
        public RestResponse<String> get401() {
            return RestResponse.status(RestResponse.Status.UNAUTHORIZED, "Unauthorized");
        }

        @Path("200")
        @GET
        public RestResponse<byte[]> get200() {
            byte[] byteArray = new byte[OK_RESPONSE_SIZE];
            Random random = new Random();
            random.nextBytes(byteArray);
            return RestResponse.ok(byteArray);
        }
    }
}
