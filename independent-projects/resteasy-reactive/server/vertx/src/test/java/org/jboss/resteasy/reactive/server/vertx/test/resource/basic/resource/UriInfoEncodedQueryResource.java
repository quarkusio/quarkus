package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;

@Path("UriInfoEncodedQueryResource/query")
public class UriInfoEncodedQueryResource {

    @GET
    public String doGet(@QueryParam("a") String a, @Context UriInfo info) {
        Assertions.assertEquals("a b", a);
        Assertions.assertEquals("a b", info.getQueryParameters().getFirst("a"));
        Assertions.assertEquals("a%20b", info.getQueryParameters(false).getFirst("a"));
        return "content";
    }
}
