package io.quarkus.vertx.web.runtime;

import javax.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
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

    private static final String REQUEST_CONTEXT_STATE = "__cdi_req_ctx";

    private final Event<SecurityIdentity> securityIdentityEvent;
    private final CurrentIdentityAssociation currentIdentityAssociation;
    private final CurrentVertxRequest currentVertxRequest;
    private final ManagedContext requestContext;

    public RouteHandler() {
        this.securityIdentityEvent = Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
        this.currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
        this.requestContext = Arc.container().requestContext();
        this.currentIdentityAssociation = Arc.container().instance(CurrentIdentityAssociation.class).get();
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
        //todo: how should we handle non-proactive authentication here?
        if (requestContext.isActive()) {
            if (currentIdentityAssociation != null) {
                if (user != null) {
                    SecurityIdentity identity = user.getSecurityIdentity();
                    currentIdentityAssociation.setIdentity(identity);
                } else {
                    currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(context, null));
                }
            }
            if (user != null) {
                securityIdentityEvent.fire(user.getSecurityIdentity());
            }
            invoke(context);
        } else {
            try {
                // First attempt to obtain the request context state.
                // If there is a route filter and the request can have body (POST, PUT, etc.) the route 
                // method is invoked asynchronously (once all data are read). 
                // However, the request context is activated by the filter and we need to make sure 
                // the same context is then used in the route method
                ContextState state = context.get(REQUEST_CONTEXT_STATE);
                // Activate the context, i.e. set the thread locals, state can be null
                requestContext.activate(state);
                currentVertxRequest.setCurrent(context);
                if (currentIdentityAssociation != null) {
                    if (user != null) {
                        SecurityIdentity identity = user.getSecurityIdentity();
                        currentIdentityAssociation.setIdentity(identity);
                    } else {
                        currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(context, null));
                    }
                }
                if (user != null) {
                    securityIdentityEvent.fire(user.getSecurityIdentity());
                }
                if (state == null) {
                    // Reactive routes can use async processing (e.g. mutiny Uni/Multi) and context propagation
                    // 1. Store the state (which is basically a shared Map instance)
                    // 2. Terminate the context correctly when the response is disposed or an exception is thrown
                    final ContextState endState = requestContext.getState();
                    context.put(REQUEST_CONTEXT_STATE, endState);
                    context.addEndHandler(new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> result) {
                            requestContext.destroy(endState);
                        }
                    });
                }
                invoke(context);
            } finally {
                // Deactivate the context, i.e. cleanup the thread locals
                requestContext.deactivate();
            }
        }
    }

}
