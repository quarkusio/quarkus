package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/uds-client-test")
public class UdsClientResource {

    @RestClient
    UdsClient client;

    @RestClient
    UdsNoUrlClient noUrlClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return client.test();
    }

    @GET
    @Path("/no-url")
    @Produces(MediaType.TEXT_PLAIN)
    public String testNoUrl() {
        return noUrlClient.test();
    }
}
