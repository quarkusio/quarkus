package io.quarkus.rest.client.reactive.error;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ResponseResultTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class).addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));

    @RestClient
    Client client;

    @Test
    void responseShouldBeProperlySet() {
        assertThat(client.test(false).getStatus()).isEqualTo(200);
        assertThat(client.test(true).getStatus()).isEqualTo(404);
    }

    @Path("/test")
    public static class Resource {
        @GET
        public Response test(@QueryParam("notFound") @DefaultValue("false") boolean notFound) {
            return Response.status(notFound ? 404 : 200).build();
        }
    }

    @Path("/test")
    @RegisterRestClient
    public interface Client {
        @GET
        Response test(@QueryParam("notFound") boolean notFound);
    }
}
