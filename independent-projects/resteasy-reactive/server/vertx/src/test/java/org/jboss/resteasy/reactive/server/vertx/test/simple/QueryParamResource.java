package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
