package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class UriInfoRelativizeResource {
    @Produces("text/plain")
    @GET
    @Path("{path : .*}")
    public String relativize(@Context UriInfo info, @QueryParam("to") String to) {
        return info.relativize(URI.create(to)).toString();
    }
}
