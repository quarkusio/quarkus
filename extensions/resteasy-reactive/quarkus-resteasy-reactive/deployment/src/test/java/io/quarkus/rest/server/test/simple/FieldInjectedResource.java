package io.quarkus.rest.server.test.simple;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
