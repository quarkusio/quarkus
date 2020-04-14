package io.quarkus.vertx.web.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles invocation of a reactive route.
 * 
 * @see Route
 */
public interface RouteHandler extends Handler<RoutingContext> {

    /**
     * Initialize the handler instance before it is put into service.
     */
    void initialize();

    /**
     * Invokes the route method.
     * 
     * @param context
     */
    void invoke(RoutingContext context);

    @Override
    default void handle(RoutingContext context) {
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            if (user != null) {
                Arc.container().beanManager().fireEvent(user.getSecurityIdentity());
            }
            invoke(context);
        } else {
            try {
                requestContext.activate();
                if (user != null) {
                    Arc.container().beanManager().fireEvent(user.getSecurityIdentity());
                }
                invoke(context);
            } finally {
                requestContext.terminate();
            }
        }
    }

}
