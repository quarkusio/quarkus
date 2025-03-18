package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.net.URI;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Link;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hal.HalService;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.resteasy.reactive.links.runtime.hal.ResteasyReactiveHalService;

public final class ResponseImplementor {

    public ResultHandle ok(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle builder = creator.invokeStaticMethod(
                ofMethod(RestResponse.ResponseBuilder.class, "ok", RestResponse.ResponseBuilder.class, Object.class), entity);
        return creator.invokeVirtualMethod(ofMethod(RestResponse.ResponseBuilder.class, "build", RestResponse.class), builder);
    }

    public ResultHandle ok(BytecodeCreator creator, ResultHandle entity, ResultHandle links) {
        ResultHandle builder = creator.invokeStaticMethod(
                ofMethod(RestResponse.ResponseBuilder.class, "ok", RestResponse.ResponseBuilder.class, Object.class), entity);
        creator.invokeVirtualMethod(
                ofMethod(RestResponse.ResponseBuilder.class, "links", RestResponse.ResponseBuilder.class, Link[].class),
                builder, links);
        return creator.invokeVirtualMethod(ofMethod(RestResponse.ResponseBuilder.class, "build", RestResponse.class), builder);
    }

    public ResultHandle created(BytecodeCreator creator, ResultHandle entity, ResourceProperties resourceProperties) {
        if (resourceProperties.isHal()) {
            return doCreated(creator, entity, getEntityUrl(creator, entity));
        }

        return doCreated(creator, entity, null);
    }

    public ResultHandle getEntityUrl(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle arcContainer = creator
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instance = creator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                arcContainer,
                creator.loadClassFromTCCL(ResteasyReactiveHalService.class),
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
        return status(creator, RestResponse.Status.NO_CONTENT.getStatusCode());
    }

    public ResultHandle notFound(BytecodeCreator creator) {
        return status(creator, RestResponse.Status.NOT_FOUND.getStatusCode());
    }

    public ResultHandle notFoundException(BytecodeCreator creator) {
        return creator.newInstance(MethodDescriptor.ofConstructor(WebApplicationException.class, int.class),
                creator.load(RestResponse.Status.NOT_FOUND.getStatusCode()));
    }

    private ResultHandle doCreated(BytecodeCreator creator, ResultHandle entity, ResultHandle location) {
        ResultHandle builder = getResponseBuilder(creator, RestResponse.Status.CREATED.getStatusCode());
        creator.invokeVirtualMethod(
                ofMethod(RestResponse.ResponseBuilder.class, "entity", RestResponse.ResponseBuilder.class, Object.class),
                builder, entity);
        if (location != null) {
            creator.invokeVirtualMethod(
                    ofMethod(RestResponse.ResponseBuilder.class, "location", RestResponse.ResponseBuilder.class, URI.class),
                    builder, location);
        }

        return creator.invokeVirtualMethod(ofMethod(RestResponse.ResponseBuilder.class, "build", RestResponse.class), builder);
    }

    private ResultHandle status(BytecodeCreator creator, int status) {
        ResultHandle builder = getResponseBuilder(creator, status);
        return creator.invokeVirtualMethod(ofMethod(RestResponse.ResponseBuilder.class, "build", RestResponse.class), builder);
    }

    private ResultHandle getResponseBuilder(BytecodeCreator creator, int status) {
        return creator.invokeStaticMethod(
                ofMethod(RestResponse.ResponseBuilder.class, "create", RestResponse.ResponseBuilder.class, int.class),
                creator.load(status));
    }

}
