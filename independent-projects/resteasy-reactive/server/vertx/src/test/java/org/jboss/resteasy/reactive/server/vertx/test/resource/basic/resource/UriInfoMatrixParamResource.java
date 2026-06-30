package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestMatrix;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/UriInfoMatrixParamResource")
public class UriInfoMatrixParamResource {

    @Path("ex")
    @GET
    public String ex(UriInfo uriInfo, @RestMatrix String m1, @RestMatrix String m2, @RestQuery String q1,
            @RestQuery String q2) {
        return "ex: " + uriInfo.getRequestUri()
                + " m1: " + m1
                + " m2: " + m2
                + " q1: " + q1
                + " q2: " + q2
                + " query params: " + uriInfo.getQueryParameters()
                + " segments: " + uriInfo.getPathSegments()
                + " matrix params: " + uriInfo.getPathSegments().get(0).getMatrixParameters();
    }
}
