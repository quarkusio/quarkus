package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@Path("/ctor-query")
public class QueryParamResource {

    private final String queryParamValue;

    public QueryParamResource(HelloService helloService, @QueryParam("q1") String queryParamValue, @Context UriInfo uriInfo) {
        this.queryParamValue = queryParamValue;
    }

    @GET
    public String get() {
        return queryParamValue;
    }
}
