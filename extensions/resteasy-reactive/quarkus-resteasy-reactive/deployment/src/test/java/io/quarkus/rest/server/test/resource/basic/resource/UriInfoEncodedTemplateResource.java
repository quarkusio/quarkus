package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

@Path("UriInfoEncodedTemplateResource/{a}/{b}")
public class UriInfoEncodedTemplateResource {
    private static final String ERROR_MSG = "Wrong parameter";

    @GET
    public String doGet(@PathParam("a") String a, @PathParam("b") String b, @Context UriInfo info) {
        Assertions.assertEquals("a b", a);
        Assertions.assertEquals("x y", b);
        Assertions.assertEquals("a b", info.getPathParameters().getFirst("a"));
        Assertions.assertEquals("x y", info.getPathParameters().getFirst("b"));
        Assertions.assertEquals("a%20b", info.getPathParameters(false).getFirst("a"));
        Assertions.assertEquals("x%20y", info.getPathParameters(false).getFirst("b"));

        List<PathSegment> decoded = info.getPathSegments(true);
        Assertions.assertEquals(decoded.size(), 2);
        Assertions.assertEquals("a b", decoded.get(0).getPath());
        Assertions.assertEquals("x y", decoded.get(1).getPath());

        List<PathSegment> encoded = info.getPathSegments(false);
        Assertions.assertEquals(encoded.size(), 2);
        Assertions.assertEquals("a%20b", encoded.get(0).getPath());
        Assertions.assertEquals("x%20y", encoded.get(1).getPath());
        return "content";
    }
}
