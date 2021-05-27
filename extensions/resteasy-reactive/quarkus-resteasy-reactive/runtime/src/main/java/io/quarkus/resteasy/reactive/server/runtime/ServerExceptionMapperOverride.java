package io.quarkus.resteasy.reactive.server.runtime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

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
@OverrideTarget(ServerExceptionMapper.class)
@TargetMethod(returnTypes = { Response.class, UniResponse.class }, parameterTypes = {
        ContainerRequestContext.class, UriInfo.class, HttpHeaders.class, Request.class,
        ResourceInfo.class, SimpleResourceInfo.class, RoutingContext.class, ThrowableSubtype.class, HttpServerRequest.class })
public class ServerExceptionMapperOverride implements DefinitionOverride {

}

class UniResponse extends TargetMethod.GenericType<Uni<Response>> {
}

class ThrowableSubtype extends TargetMethod.Subtype<Throwable> {
}