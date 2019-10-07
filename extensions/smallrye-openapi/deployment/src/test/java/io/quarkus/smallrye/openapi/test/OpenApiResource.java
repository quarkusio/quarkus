package io.quarkus.smallrye.openapi.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/resource")
public class OpenApiResource {

    @GET
    public String root() {
        return "resource";
    }

    @GET
    @Path("/test-enums")
    public Query testEnums(@QueryParam("query") Query query) {
        return query;
    }

    public enum Query {
        QUERY_PARAM_1,
        QUERY_PARAM_2;
    }
}
