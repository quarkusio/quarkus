package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
