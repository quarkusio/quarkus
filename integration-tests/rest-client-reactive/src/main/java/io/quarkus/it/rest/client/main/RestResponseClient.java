package io.quarkus.it.rest.client.main;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

@Path("")
public interface RestResponseClient {

    @Produces(MediaType.TEXT_PLAIN)
    @GET
    RestResponse<String> response();
}
