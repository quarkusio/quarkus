package io.quarkus.it.mockbean;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface GreetingRestClient {
    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    String greeting();
}
