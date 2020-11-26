package io.quarkus.rest.server.test.perclassexception;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;

import io.vertx.core.http.HttpServerRequest;

@Path("first")
public class FirstResource {

    @GET
    @Produces("text/plain")
    public String throwsVariousExceptions(@RestQuery String name) {
        if (name.startsWith("IllegalArgument")) {
            throw new IllegalArgumentException();
        } else if (name.startsWith("IllegalState")) {
            throw new IllegalStateException("IllegalState");
        } else if (name.startsWith("My")) {
            throw new MyException();
        }
        throw new RuntimeException();
    }

    @ServerExceptionMapper({ IllegalStateException.class, IllegalArgumentException.class })
    public Response handleIllegal() {
        return Response.status(409).build();
    }

    @ServerExceptionMapper(MyException.class)
    public Response handleMy(SimplifiedResourceInfo simplifiedResourceInfo, MyException myException,
            ContainerRequestContext containerRequestContext, UriInfo uriInfo, HttpHeaders httpHeaders, Request request,
            HttpServerRequest httpServerRequest) {
        return Response.status(410).entity(uriInfo.getPath() + "->" + simplifiedResourceInfo.getMethodName()).build();
    }
}
