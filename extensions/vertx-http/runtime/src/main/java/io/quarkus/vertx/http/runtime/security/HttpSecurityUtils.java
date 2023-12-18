package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.isExplicitlyMarkedAsUnsafe;
import static io.smallrye.common.vertx.VertxContext.isDuplicatedContext;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.request.AuthenticationRequest;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public final class HttpSecurityUtils {
    public final static String ROUTING_CONTEXT_ATTRIBUTE = "quarkus.http.routing.context";
    private static final Logger LOG = Logger.getLogger(HttpSecurityUtils.class);

    private HttpSecurityUtils() {

    }

    public static AuthenticationRequest setRoutingContextAttribute(AuthenticationRequest request, RoutingContext context) {
        request.setAttribute(ROUTING_CONTEXT_ATTRIBUTE, context);
        return request;
    }

    public static RoutingContext getRoutingContextAttribute(AuthenticationRequest request) {
        return request.getAttribute(ROUTING_CONTEXT_ATTRIBUTE);
    }

    /**
     * Add {@link RoutingContext} to Vert.x duplicated context local data.
     */
    public static void setRoutingContextAttribute(RoutingContext event) {
        final Context context = Vertx.currentContext();
        if (context != null && context.getLocal(RoutingContext.class.getName()) == null) {
            if (isSafeToUseContext(context)) {
                context.putLocal(RoutingContext.class.getName(), event);
            } else {
                LOG.debug("""
                        RoutingContext not added to Vert.x context as it is not safe to use the context local data.
                        It won't be possible to access RoutingContext with 'HttpSecurityUtils.getRoutingContextAttribute()'.
                        """);
            }
        }
    }

    /**
     * @return RoutingContext if present in Vert.x duplicated context local data
     */
    public static RoutingContext getRoutingContextAttribute() {
        final Context context = Vertx.currentContext();
        return context != null && isSafeToUseContext(context) ? context.getLocal(RoutingContext.class.getName()) : null;
    }

    private static boolean isSafeToUseContext(io.vertx.core.Context context) {
        return isDuplicatedContext(context) && !isExplicitlyMarkedAsUnsafe(context);
    }
}
