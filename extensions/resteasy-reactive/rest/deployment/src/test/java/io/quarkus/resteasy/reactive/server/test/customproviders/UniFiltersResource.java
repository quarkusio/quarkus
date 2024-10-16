package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("uni")
public class UniFiltersResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("req")
    public String filters(@Context HttpHeaders headers) {
        return headers.getHeaderString("custom-uni-header");
    }
}
