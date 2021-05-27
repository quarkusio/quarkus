package io.quarkus.resteasy.reactive.server.runtime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.smallrye.safer.annotations.DefinitionOverride;
import io.smallrye.safer.annotations.OverrideTarget;
import io.smallrye.safer.annotations.TargetMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Adds the following allowed parameters to the Safer-Annotations type checking
 * - RoutingContext
 * - HttpServerRequest
 */
@OverrideTarget(ServerResponseFilter.class)
@TargetMethod(returnTypes = { void.class, UniVoid.class }, parameterTypes = {
        ContainerRequestContext.class,
        ContainerResponseContext.class,
        ResteasyReactiveContainerRequestContext.class,
        ResourceInfo.class, SimpleResourceInfo.class, Throwable.class, RoutingContext.class,
        HttpServerRequest.class })
public class ServerResponseFilterOverride implements DefinitionOverride {

}
