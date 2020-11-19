package io.quarkus.vertx.web.runtime;

import javax.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.web.Route;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles invocation of a reactive route.
 * 
 * @see Route
 */
public abstract class RouteHandler implements Handler<RoutingContext> {

    private final Event<SecurityIdentity> securityIdentityEvent;
    private final CurrentVertxRequest currentVertxRequest;

    public RouteHandler() {
        this.securityIdentityEvent = Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
        this.currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
    }

    /**
     * Invokes the route method.
     * 
     * @param context
     */
    public abstract void invoke(RoutingContext context);

    @Override
    public void handle(RoutingContext context) {
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();
        ManagedContext requestContext = Arc.container().requestContext();
        //todo: how should we handle non-proactive authentication here?
        if (requestContext.isActive()) {
            if (user != null) {
                securityIdentityEvent.fire(user.getSecurityIdentity());
            }
            invoke(context);
        } else {
            try {
                // Activate the context, i.e. set the thread locals
                requestContext.activate();
                currentVertxRequest.setCurrent(context);
                if (user != null) {
                    securityIdentityEvent.fire(user.getSecurityIdentity());
                }
                // Reactive routes can use async processing (e.g. mutiny Uni/Multi) and context propagation
                // 1. Store the state (which is basically a shared Map instance)
                // 2. Terminate the context correcly when the response is disposed or an exception is thrown 
                InjectableContext.ContextState state = requestContext.getState();
                context.addEndHandler(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> result) {
                        requestContext.destroy(state);
                    }
                });
                invoke(context);
            } finally {
                // Deactivate the context, i.e. cleanup the thread locals
                requestContext.deactivate();
            }
        }
    }

}
