package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

public abstract class AbstractClientMaskedHeaderLogTest {

    @Path("resource")
    @RegisterRestClient(configKey = "my-client")
    public interface Client {

        @Path("/hello")
        @GET
        String hello(@HeaderParam("Authorization") String auth, @HeaderParam("x-requested-locale") String requestedLocale);
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public RestResponse<Object> hello() {
            return RestResponse.ResponseBuilder.ok().header("x-secret", "super-sensitive-value")
                    .header("x-locale", "de-DE").build();
        }
    }
}
