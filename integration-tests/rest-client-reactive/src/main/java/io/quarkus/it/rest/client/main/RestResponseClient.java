package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

@Path("")
public interface RestResponseClient {

    @Produces(MediaType.TEXT_PLAIN)
    @GET
    RestResponse<String> response();
}
