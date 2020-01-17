package io.quarkus.panache.rest.common.deployment.utils;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public final class ResponseImplementor {

    public static ResultHandle created(BytecodeCreator creator, ResultHandle entity, ResultHandle location) {
        ResultHandle builder = getResponseBuilder(creator, Response.Status.CREATED.getStatusCode());

        creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ResponseBuilder.class, "entity", ResponseBuilder.class, Object.class), builder,
                entity);
        creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ResponseBuilder.class, "location", ResponseBuilder.class, URI.class), builder,
                location);

        return creator.invokeVirtualMethod(MethodDescriptor.ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    public static ResultHandle noContent(BytecodeCreator creator) {
        return status(creator, Response.Status.NO_CONTENT.getStatusCode());
    }

    public static ResultHandle notFoundException(BytecodeCreator creator) {
        return creator.newInstance(MethodDescriptor.ofConstructor(WebApplicationException.class, int.class),
                creator.load(Response.Status.NOT_FOUND.getStatusCode()));
    }

    private static ResultHandle status(BytecodeCreator creator, int status) {
        ResultHandle builder = getResponseBuilder(creator, status);

        return creator.invokeVirtualMethod(MethodDescriptor.ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    private static ResultHandle getResponseBuilder(BytecodeCreator creator, int status) {
        return creator.invokeStaticMethod(
                MethodDescriptor.ofMethod(Response.class, "status", ResponseBuilder.class, int.class), creator.load(status));
    }
}
