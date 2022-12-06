package io.quarkus.resteasy.reactive.server.runtime;

import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.ext.web.RoutingContext;

public class QuarkusResteasyReactiveRequestContext extends VertxResteasyReactiveRequestContext {

    final CurrentIdentityAssociation association;
    boolean userSetup = false;

    public QuarkusResteasyReactiveRequestContext(Deployment deployment,
            RoutingContext context, ThreadSetupAction requestContext, ServerRestHandler[] handlerChain,
            ServerRestHandler[] abortHandlerChain, ClassLoader devModeTccl,
            CurrentIdentityAssociation currentIdentityAssociation) {
        super(deployment, context, requestContext, handlerChain, abortHandlerChain, devModeTccl);
        this.association = currentIdentityAssociation;
        if (VertxContext.isOnDuplicatedContext()) {
            VertxContextSafetyToggle.setCurrentContextSafe(true);
        }
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

    @Override
    protected void requestScopeDeactivated() {
        // we intentionally don't call 'CurrentRequestManager.set(null)'
        // because there is no need to clear the current request
        // as that is backed by a DuplicatedContext and not accessible to other requests anyway
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

    /**
     * The implementation looks like it makes no sense, but it in fact does make sense from a performance perspective.
     * The idea is to reduce the use instances of megamorphic calls into a series of instance checks and monomorphic calls.
     * The rationale behind this is fully explored in
     * https://shipilev.net/blog/2015/black-magic-method-dispatch/#_cheating_the_runtime_2
     * and this specific instance has been verified experimentally to result in better performance.
     */
    @Override
    protected void invokeHandler(int pos) throws Exception {
        var handler = handlers[pos];
        if (handler instanceof org.jboss.resteasy.reactive.server.handlers.MatrixParamHandler) {
            handler.handle(this);
        } else if (handler instanceof io.quarkus.resteasy.reactive.server.runtime.security.SecurityContextOverrideHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.RestInitialHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.AbortChainHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.NonBlockingHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.BlockingHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InputHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ParameterHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InstanceHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InvocationHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.FixedProducesHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResponseHandler) {
            handler.handle(this);
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler) {
            handler.handle(this);
        } else {
            // megamorphic call for other handlers
            handler.handle(this);
        }
    }
}
