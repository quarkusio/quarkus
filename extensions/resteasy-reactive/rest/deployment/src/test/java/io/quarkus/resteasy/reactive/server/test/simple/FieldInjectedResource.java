package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

@Path("injection")
public class FieldInjectedResource {

    @QueryParam("query")
    String query;

    @HeaderParam("header")
    String header;

    @Context
    UriInfo uriInfo;

    @BeanParam
    SimpleBeanParam beanParam;

    @Path("field")
    @GET
    public String field() {
        checkInjections("/injection/field", query, header, uriInfo, beanParam);
        return "OK";
    }

    protected void checkInjections(String path, String query, String header, UriInfo uriInfo, SimpleBeanParam beanParam) {
        Assertions.assertEquals("one-query", query);
        Assertions.assertEquals("one-header", header);
        Assertions.assertNotNull(uriInfo);
        Assertions.assertEquals(path, uriInfo.getPath());
        Assertions.assertNotNull(beanParam);
        beanParam.check(path);
    }

    @Path("param")
    @GET
    public String param(@QueryParam("query") String query,
            @HeaderParam("header") String header,
            @Context UriInfo uriInfo,
            @BeanParam SimpleBeanParam beanParam) {
        checkInjections("/injection/param", query, header, uriInfo, beanParam);
        return "OK";
    }
}
