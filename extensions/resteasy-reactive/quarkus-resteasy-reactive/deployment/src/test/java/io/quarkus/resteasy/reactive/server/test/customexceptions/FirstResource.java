package io.quarkus.resteasy.reactive.server.test.customexceptions;

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
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.smallrye.mutiny.Uni;
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
        } else if (name.startsWith("MyOther")) {
            throw new MyOtherException();
        } else if (name.startsWith("My")) {
            throw new MyException();
        } else if (name.startsWith("Uni")) {
            throw new UniException();
        }
        throw new RuntimeException();
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
        return Uni.createFrom().failure(e);
    }

    @ServerExceptionMapper({ IllegalStateException.class, IllegalArgumentException.class })
    public Response handleIllegal() {
        return Response.status(409).build();
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
