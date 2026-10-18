package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/custom")
public class CustomFiltersResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("req")
    public String filters(@Context HttpHeaders headers) {
        return headers.getHeaderString("custom-header") + "-" + headers.getHeaderString("heavy");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("metal")
    @Metal
    public String metal(@Context HttpHeaders headers) {
        return headers.getHeaderString("custom-header") + "-" + headers.getHeaderString("heavy");
    }

}
