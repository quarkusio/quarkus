package io.quarkus.rest.test.simple;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
        return "query=" + query + ", header=" + header + ", uriInfo.path=" + uriInfo.getPath()
                + ", beanParam.query=" + beanParam.query + ", beanParam.header=" + beanParam.header
                + ", beanParam.uriInfo.path=" + beanParam.uriInfo.getPath()
                + ", beanParam.otherBeanParam.query=" + beanParam.otherBeanParam.query + ", beanParam.otherBeanParam.header="
                + beanParam.otherBeanParam.header
                + ", beanParam.otherBeanParam.uriInfo.path=" + beanParam.otherBeanParam.uriInfo.getPath();
    }

    @Path("param")
    @GET
    public String param(@QueryParam("query") String query,
            @HeaderParam("header") String header,
            @Context UriInfo uriInfo,
            @BeanParam SimpleBeanParam beanParam) {
        return "query=" + query + ", header=" + header + ", uriInfo.path=" + uriInfo.getPath()
                + ", beanParam.query=" + beanParam.query + ", beanParam.header=" + beanParam.header
                + ", beanParam.uriInfo.path=" + beanParam.uriInfo.getPath()
                + ", beanParam.otherBeanParam.query=" + beanParam.otherBeanParam.query + ", beanParam.otherBeanParam.header="
                + beanParam.otherBeanParam.header
                + ", beanParam.otherBeanParam.uriInfo.path=" + beanParam.otherBeanParam.uriInfo.getPath();
    }
}
