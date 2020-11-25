package io.quarkus.rest.server.test.simple;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

public class OtherBeanParam {
    @QueryParam("query")
    String query;

    @HeaderParam("header")
    String header;

    @Context
    UriInfo uriInfo;

    public void check(String path) {
        Assertions.assertEquals("one-query", query);
        Assertions.assertEquals("one-header", header);
        Assertions.assertNotNull(uriInfo);
        Assertions.assertEquals(path, uriInfo.getPath());
    }
}
