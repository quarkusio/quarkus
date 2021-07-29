package io.quarkus.rest.client.reactive.error;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ErrorMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Dto.class, Client.class, Resource.class).addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));
    public static final String ERROR_MESSAGE = "The entity was not found";

    @RestClient
    Client client;

    @Test
    void shouldHandleEntityBody() {
        try {
            client.get();
            fail("No exception thrown by a REST call that returns 404");
        } catch (WebApplicationException expected) {
            Response response = expected.getResponse();
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(404);
            String message = response.readEntity(String.class);
            assertThat(message).isEqualTo(ERROR_MESSAGE);
        }
    }

    public static class Dto {
        public String field;
    }

    @Path("/error")
    @RegisterRestClient
    public interface Client {
        @GET
        Dto get();
    }

    @Path("/error")
    public static class Resource {
        @GET
        public Response returnError() {
            return Response.status(404).entity(ERROR_MESSAGE).build();
        }
    }

}
