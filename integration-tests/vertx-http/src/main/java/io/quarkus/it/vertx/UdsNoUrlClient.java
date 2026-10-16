package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Same as {@link UdsClient} but configured with only a domain socket and no URL.
 */
@Path("/uds")
@RegisterRestClient(configKey = "uds-no-url")
public interface UdsNoUrlClient {

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    String test();
}
