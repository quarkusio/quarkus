package io.quarkus.rest.server.runtime;

import java.util.function.Supplier;

import javax.enterprise.event.Event;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

public class QuarkusResteasyReactiveRequestContext extends VertxResteasyReactiveRequestContext {

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            new Supplier<Event<SecurityIdentity>>() {
                @Override
                public Event<SecurityIdentity> get() {
                    return QuarkusResteasyReactiveRequestContext.createEvent();
                }
            });

    public QuarkusResteasyReactiveRequestContext(Deployment deployment, QuarkusRestProviders providers,
            RoutingContext context, ThreadSetupAction requestContext, ServerRestHandler[] handlerChain,
            ServerRestHandler[] abortHandlerChain) {
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
        return new ResteasyReactiveSecurityContext(context);
    }

}
