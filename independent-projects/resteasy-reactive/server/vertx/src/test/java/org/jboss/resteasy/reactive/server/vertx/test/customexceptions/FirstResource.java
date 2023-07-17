package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

@Path("first")
public class FirstResource {

    @GET
    @Produces("text/plain")
    public String throwsVariousExceptions(@RestQuery String name) {
        if (name.startsWith("IllegalArgument")) {
            throw removeStackTrace(new IllegalArgumentException());
        } else if (name.startsWith("IllegalState")) {
            throw removeStackTrace(new IllegalStateException("IllegalState"));
        } else if (name.startsWith("MyOther")) {
            throw removeStackTrace(new MyOtherException());
        } else if (name.startsWith("My")) {
            throw removeStackTrace(new MyException());
        } else if (name.startsWith("Uni")) {
            throw removeStackTrace(new UniException());
        }
        throw removeStackTrace(new RuntimeException());
    }

    @GET
    @Path("uni")
    @Produces("text/plain")
    public Uni<String> uni(@RestQuery String name) {
        Exception e = new RuntimeException();
        if (name.startsWith("IllegalArgument")) {
            e = new IllegalArgumentException();
        } else if (name.startsWith("IllegalState")) {
            e = new IllegalStateException("IllegalState");
        } else if (name.startsWith("MyOther")) {
            e = new MyOtherException();
        } else if (name.startsWith("My")) {
            e = new MyException();
        } else if (name.startsWith("Uni")) {
            e = new UniException();
        }
        return Uni.createFrom().failure(removeStackTrace(e));
    }

    @ServerExceptionMapper({ IllegalStateException.class, IllegalArgumentException.class })
    public Response handleIllegal(Exception e) {
        return Response.status(409).entity(e.getMessage()).build();
    }

    @ServerExceptionMapper
    public Response handleMyException(SimpleResourceInfo simplifiedResourceInfo, MyException myException,
            ContainerRequestContext containerRequestContext, UriInfo uriInfo, HttpHeaders httpHeaders, Request request,
            HttpServerRequest httpServerRequest) {
        return Response.status(410).entity(uriInfo.getPath() + "->" + simplifiedResourceInfo.getMethodName()).build();
    }

    @ServerExceptionMapper(UniException.class)
    public Uni<Response> handleUniException(UniException e, UriInfo uriInfo, SimpleResourceInfo simplifiedResourceInfo) {
        return Uni.createFrom().item(
                () -> Response.status(412).entity(uriInfo.getPath() + "->" + simplifiedResourceInfo.getMethodName()).build());
    }
}
