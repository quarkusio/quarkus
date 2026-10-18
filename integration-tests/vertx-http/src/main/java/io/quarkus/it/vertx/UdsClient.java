package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/uds")
@RegisterRestClient(configKey = "uds")
public interface UdsClient {

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    String test();
}
