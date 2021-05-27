package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.smallrye.mutiny.Uni;
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
@OverrideTarget(ServerRequestFilter.class)
@TargetMethod(returnTypes = { void.class, Response.class, UniResponse.class, UniVoid.class,
        OptionalResponse.class }, parameterTypes = {
                ContainerRequestContext.class, UriInfo.class, HttpHeaders.class, Request.class,
                ResourceInfo.class, SimpleResourceInfo.class, ResteasyReactiveContainerRequestContext.class,
                RoutingContext.class,
                HttpServerRequest.class })
public class ServerRequestFilterOverride implements DefinitionOverride {

}

class UniVoid extends TargetMethod.GenericType<Uni<Void>> {
}

class OptionalResponse extends TargetMethod.GenericType<Optional<Response>> {
}
