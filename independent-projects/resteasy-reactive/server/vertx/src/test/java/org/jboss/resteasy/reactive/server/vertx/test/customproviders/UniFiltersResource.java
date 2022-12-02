package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("uni")
public class UniFiltersResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("req")
    public String filters(@Context HttpHeaders headers) {
        return headers.getHeaderString("custom-uni-header");
    }
}
