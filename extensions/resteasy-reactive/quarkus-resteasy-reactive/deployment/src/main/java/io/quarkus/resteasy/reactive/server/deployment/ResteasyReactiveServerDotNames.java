package io.quarkus.resteasy.reactive.server.deployment;

import javax.ws.rs.core.Context;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveServerDotNames {
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName SERVER_REQUEST_FILTER = DotName
            .createSimple(ServerRequestFilter.class.getName());
    public static final DotName SERVER_RESPONSE_FILTER = DotName
            .createSimple(ServerResponseFilter.class.getName());
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple(ResteasyReactiveContainerRequestContext.class.getName());
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimpleResourceInfo.class.getName());
    public static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());

}
