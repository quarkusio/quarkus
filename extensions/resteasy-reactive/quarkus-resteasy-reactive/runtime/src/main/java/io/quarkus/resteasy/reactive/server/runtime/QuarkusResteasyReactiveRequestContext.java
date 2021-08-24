package io.quarkus.resteasy.reactive.server.runtime;

import javax.enterprise.event.Event;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

public class QuarkusResteasyReactiveRequestContext extends VertxResteasyReactiveRequestContext {

    final CurrentIdentityAssociation association;
    boolean userSetup = false;

    public QuarkusResteasyReactiveRequestContext(Deployment deployment, ProvidersImpl providers,
            RoutingContext context, ThreadSetupAction requestContext, ServerRestHandler[] handlerChain,
            ServerRestHandler[] abortHandlerChain, ClassLoader devModeTccl,
            CurrentIdentityAssociation currentIdentityAssociation) {
        super(deployment, providers, context, requestContext, handlerChain, abortHandlerChain, devModeTccl);
        this.association = currentIdentityAssociation;
    }

    protected void handleRequestScopeActivation() {
        super.handleRequestScopeActivation();
        if (!userSetup && association != null) {
            userSetup = true;
            QuarkusHttpUser existing = (QuarkusHttpUser) context.user();
            if (existing != null) {
                SecurityIdentity identity = existing.getSecurityIdentity();
                association.setIdentity(identity);
            } else {
                association.setIdentity(QuarkusHttpUser.getSecurityIdentity(context, null));
            }
        }
    }

    private static Event<SecurityIdentity> createEvent() {
        return Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
    }

    protected SecurityContext createSecurityContext() {
        return new ResteasyReactiveSecurityContext(context);
    }

    @Override
    protected void handleUnrecoverableError(Throwable throwable) {
        context.fail(throwable);
        super.handleUnrecoverableError(throwable);
    }

    @Override
    public boolean handlesUnmappedException() {
        return false; // false because handleUnmappedException just throws and lets QuarkusErrorHandler return the final response
    }

    @Override
    public void handleUnmappedException(Throwable throwable) {
        throw sneakyThrow(throwable);
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
