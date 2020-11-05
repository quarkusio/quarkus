package io.quarkus.rest.server.runtime;

import javax.enterprise.event.Event;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.common.runtime.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRequestContext extends ResteasyReactiveRequestContext {

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            QuarkusRequestContext::createEvent);

    public QuarkusRequestContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers, RoutingContext context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
        super(deployment, providers, context, requestContext, handlerChain, abortHandlerChain);
    }

    protected void handleRequestScopeActivation() {
        super.handleRequestScopeActivation();
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();
        if (user != null) {
            fireSecurityIdentity(user.getSecurityIdentity());
        }
    }

    static void fireSecurityIdentity(SecurityIdentity identity) {
        SECURITY_IDENTITY_EVENT.get().fire(identity);
    }

    static void clear() {
        SECURITY_IDENTITY_EVENT.clear();
    }

    private static Event<SecurityIdentity> createEvent() {
        return Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
    }

    protected SecurityContext createSecurityContext() {
        return new QuarkusRestSecurityContext(context);
    }
}
