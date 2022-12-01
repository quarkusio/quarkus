package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.request.AuthenticationRequest;
import io.vertx.ext.web.RoutingContext;

public final class HttpSecurityUtils {
    public final static String ROUTING_CONTEXT_ATTRIBUTE = "quarkus.http.routing.context";

    private HttpSecurityUtils() {

    }

    public static AuthenticationRequest setRoutingContextAttribute(AuthenticationRequest request, RoutingContext context) {
        request.setAttribute(ROUTING_CONTEXT_ATTRIBUTE, context);
        return request;
    }

    public static RoutingContext getRoutingContextAttribute(AuthenticationRequest request) {
        return request.getAttribute(ROUTING_CONTEXT_ATTRIBUTE);
    }
}
