package io.quarkus.resteasy.reactive.server.test.customproviders;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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
