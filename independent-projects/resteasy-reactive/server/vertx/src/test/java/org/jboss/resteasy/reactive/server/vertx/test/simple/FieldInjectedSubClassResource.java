package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;

@Path("injection-subclass")
public class FieldInjectedSubClassResource extends FieldInjectedResource {

    @QueryParam("query")
    String queryInSubClass;

    @Context
    UriInfo uriInfoInSubClass;

    @BeanParam
    BeanParamSubClass beanParamSubClass;

    @Path("field2")
    @GET
    public String field() {
        checkInjections("/injection-subclass/field2", query, header, uriInfo, beanParam, queryInSubClass, uriInfoInSubClass,
                beanParamSubClass);
        return "OK";
    }

    private void checkInjections(String path, String query, String header, UriInfo uriInfo, SimpleBeanParam beanParam,
            String queryInSubClass, UriInfo uriInfoInSubClass, BeanParamSubClass beanParamSubClass) {
        super.checkInjections(path, query, header, uriInfo, beanParam);
        Assertions.assertEquals("one-query", queryInSubClass);
        Assertions.assertNotNull(uriInfoInSubClass);
        Assertions.assertEquals(path, uriInfoInSubClass.getPath());
        Assertions.assertNotNull(beanParamSubClass);
        beanParamSubClass.check(path);
    }

    @Path("param2")
    @GET
    public String param(@QueryParam("query") String query,
            @HeaderParam("header") String header,
            @Context UriInfo uriInfo,
            @BeanParam @NotNull BeanParamSubClass beanParam) {
        checkInjections("/injection-subclass/param2", query, header, uriInfo, this.beanParam, query, uriInfo, beanParam);
        return "OK";
    }
}
