package io.quarkus.rest.client.reactive;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoContentResponseTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));

    @RestClient
    Client client;

    @Test
    public void testGetStreamNoContent() {
        assertNull(client.getStream());
    }

    @Test
    public void testGetResponseNoContent() {
        Response response = client.getResponse();
        assertFalse(response.hasEntity());
        assertNull(response.getEntity());
    }

    @Path("/test")
    @RegisterRestClient
    @Produces(MediaType.TEXT_PLAIN)
    interface Client {
        @GET
        InputStream getStream();

        @GET
        Response getResponse();
    }

    @Path("/test")
    static class Resource {
        @GET
        public Response get() {
            return Response.noContent().build();
        }
    }
}
