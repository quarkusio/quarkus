package io.quarkus.rest.client.reactive.jackson.test;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * Tests that a 400 response from jackson on the client is not propagated to the server as a 400,
 * but is instead reported as an internal server error
 */
public class BadRequestNotPropagatedTestCase {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newBuilder().build();
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void testBadRequest() {
        Response data = client.target(url.toExternalForm() + "/bad-server").request().get();
        Assertions.assertEquals(500, data.getStatus());
    }

    @Path("/bad")
    public static class Bad {

        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        @GET
        public JsonObject get(List<JsonObject> o) {
            return o.get(0);
        }
    }

    @Path("/bad")
    @RegisterRestClient(baseUri = "http://localhost:8081")
    public interface BadClient {

        @Produces(MediaType.APPLICATION_JSON)
        @GET
        JsonObject get(String json);
    }

    static class JsonObject {
        String name;
    }

    @Path("/bad-server")
    public static class BadServer {

        @Inject
        @RestClient
        BadClient badClient;

        @GET
        public JsonObject get() {
            try {
                return badClient.get("{name:foo}");
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() != 400) {
                    //this is a bit odd, but we are trying to test that a 400 from jackson won't cause a 400
                    //response from the server part
                    //returning this will cause a 204 response and fail the test, as if the original exception is
                    //not 400 then something has gone wrong
                    return null;
                }
                throw e;
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }
    }
}
