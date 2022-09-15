package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.net.URI;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hal.HalService;
import io.quarkus.resteasy.links.runtime.hal.ResteasyHalService;
import io.quarkus.resteasy.reactive.links.runtime.hal.ResteasyReactiveHalService;

public final class ResponseImplementor {

    private final boolean isResteasyClassic;

    public ResponseImplementor(boolean isResteasyClassic) {
        this.isResteasyClassic = isResteasyClassic;
    }

    public ResultHandle ok(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle builder = creator.invokeStaticMethod(
                ofMethod(Response.class, "ok", ResponseBuilder.class, Object.class), entity);
        return creator.invokeVirtualMethod(ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    public ResultHandle ok(BytecodeCreator creator, ResultHandle entity, ResultHandle links) {
        ResultHandle builder = creator.invokeStaticMethod(
                ofMethod(Response.class, "ok", ResponseBuilder.class, Object.class), entity);
        creator.invokeVirtualMethod(
                ofMethod(ResponseBuilder.class, "links", ResponseBuilder.class, Link[].class), builder, links);
        return creator.invokeVirtualMethod(ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    public ResultHandle created(BytecodeCreator creator, ResultHandle entity) {
        return created(creator, entity, getEntityUrl(creator, entity));
    }

    public ResultHandle created(BytecodeCreator creator, ResultHandle entity, ResultHandle location) {
        ResultHandle builder = getResponseBuilder(creator, Response.Status.CREATED.getStatusCode());
        creator.invokeVirtualMethod(
                ofMethod(ResponseBuilder.class, "entity", ResponseBuilder.class, Object.class), builder, entity);
        creator.invokeVirtualMethod(
                ofMethod(ResponseBuilder.class, "location", ResponseBuilder.class, URI.class), builder, location);
        return creator.invokeVirtualMethod(ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    public ResultHandle getEntityUrl(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle arcContainer = creator
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instance = creator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                arcContainer,
                creator.loadClassFromTCCL(isResteasyClassic ? ResteasyHalService.class : ResteasyReactiveHalService.class),
                creator.loadNull());
        ResultHandle halService = creator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                instance);
        ResultHandle link = creator.invokeVirtualMethod(
                ofMethod(HalService.class, "getSelfLink", String.class, Object.class), halService,
                entity);
        creator.ifNull(link).trueBranch().throwException(RuntimeException.class, "Could not extract a new entity URL");
        return creator.invokeStaticMethod(ofMethod(URI.class, "create", URI.class, String.class), link);
    }

    public ResultHandle noContent(BytecodeCreator creator) {
        return status(creator, Response.Status.NO_CONTENT.getStatusCode());
    }

    public ResultHandle notFound(BytecodeCreator creator) {
        return status(creator, Response.Status.NOT_FOUND.getStatusCode());
    }

    public ResultHandle notFoundException(BytecodeCreator creator) {
        return creator.newInstance(MethodDescriptor.ofConstructor(WebApplicationException.class, int.class),
                creator.load(Response.Status.NOT_FOUND.getStatusCode()));
    }

    private ResultHandle status(BytecodeCreator creator, int status) {
        ResultHandle builder = getResponseBuilder(creator, status);
        return creator.invokeVirtualMethod(ofMethod(ResponseBuilder.class, "build", Response.class), builder);
    }

    private ResultHandle getResponseBuilder(BytecodeCreator creator, int status) {
        return creator.invokeStaticMethod(
                ofMethod(Response.class, "status", ResponseBuilder.class, int.class), creator.load(status));
    }
}
