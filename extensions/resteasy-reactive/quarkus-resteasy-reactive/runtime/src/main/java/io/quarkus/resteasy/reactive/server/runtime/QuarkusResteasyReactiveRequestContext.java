package io.quarkus.resteasy.reactive.server.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.InvocationHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
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

    private static final int MATRIX_PARAM_HANDLER = 0;
    private static final int SECURITY_CONTEXT_OVERRIDE_HANDLER = 1;
    private static final int REST_INITIAL_HANDLER = 2;
    private static final int CLASS_ROUTING_HANDLER = 3;
    private static final int ABORT_CHAIN_HANDLER = 4;
    private static final int NON_BLOCKING_HANDLER = 5;
    private static final int BLOCKING_HANDLER = 6;
    private static final int RESOURCE_REQUEST_FILTER_HANDLER = 7;
    private static final int INPUT_HANDLER = 8;
    private static final int REQUEST_DESERIALIZE_HANDLER = 9;
    private static final int PARAMETER_HANDLER = 10;
    private static final int INSTANCE_HANDLER = 11;
    private static final int INVOCATION_HANDLER = 12;
    private static final int FIXED_PRODUCES_HANDLER = 13;
    private static final int RESPONSE_HANDLER = 14;
    private static final int RESPONSE_WRITER_HANDLER = 15;

    final CurrentIdentityAssociation association;
    private int[] knownHandlerTypeMap;
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
        context.fail(effectiveThrowableForQuarkusLogging(throwable)); // this results in io.quarkus.vertx.http.runtime.QuarkusErrorHandler logging the error
        endResponse(); // we just want to end the response, nothing more
    }

    // this is a massive hack to get QuarkusErrorHandler to log an IOException when it comes from user code
    private Throwable effectiveThrowableForQuarkusLogging(Throwable throwable) {
        if (!(throwable instanceof IOException)) {
            return throwable;
        }
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int depth = 0;
        boolean convertException = false;
        while ((depth < stackTrace.length) && (depth < 5)) { // only check a few of the top frames
            StackTraceElement stackTraceElement = stackTrace[depth];
            String className = stackTraceElement.getClassName();
            if (className.contains(InvocationHandler.class.getSimpleName()) || className.contains(
                    ResourceRequestFilterHandler.class.getSimpleName())) { // TODO: we may need more here
                convertException = true;
                break;
            }
            depth++;
        }
        return convertException ? new UncheckedIOException((IOException) throwable) : throwable;

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

    @Override
    protected void setHandlers(final ServerRestHandler[] newHandlerChain) {
        super.setHandlers(newHandlerChain);
        // recompute the known handler type map, trying to avoid reallocating it if possible
        var handlers = this.handlers;
        var handlerTypePerPosition = (knownHandlerTypeMap == null || handlers.length != knownHandlerTypeMap.length) ?
                new int[newHandlerChain.length] : knownHandlerTypeMap;
        for (int position = 0; position < handlers.length; position++) {
            handlerTypePerPosition[position] = knownTypeIdFor(handlers[position]);
        }
        this.knownHandlerTypeMap = handlerTypePerPosition;
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
        var handlerType = knownHandlerTypeMap[pos];
        switch (handlerType) {
            case MATRIX_PARAM_HANDLER:
                handler.handle(this);
                break;
            case SECURITY_CONTEXT_OVERRIDE_HANDLER:
                handler.handle(this);
                break;
            case REST_INITIAL_HANDLER:
                handler.handle(this);
                break;
            case CLASS_ROUTING_HANDLER:
                handler.handle(this);
                break;
            case ABORT_CHAIN_HANDLER:
                handler.handle(this);
                break;
            case NON_BLOCKING_HANDLER:
                handler.handle(this);
                break;
            case BLOCKING_HANDLER:
                handler.handle(this);
                break;
            case RESOURCE_REQUEST_FILTER_HANDLER:
                handler.handle(this);
                break;
            case INPUT_HANDLER:
                handler.handle(this);
                break;
            case REQUEST_DESERIALIZE_HANDLER:
                handler.handle(this);
                break;
            case PARAMETER_HANDLER:
                handler.handle(this);
                break;
            case INSTANCE_HANDLER:
                handler.handle(this);
                break;
            case INVOCATION_HANDLER:
                handler.handle(this);
                break;
            case FIXED_PRODUCES_HANDLER:
                handler.handle(this);
                break;
            case RESPONSE_HANDLER:
                handler.handle(this);
                break;
            case RESPONSE_WRITER_HANDLER:
                handler.handle(this);
                break;
            default:
                // megamorphic call for other handlers
                handler.handle(this);
        }
    }

    private static int knownTypeIdFor(Object handler) {
        if (handler instanceof org.jboss.resteasy.reactive.server.handlers.MatrixParamHandler) {
            return MATRIX_PARAM_HANDLER;
        } else if (handler instanceof io.quarkus.resteasy.reactive.server.runtime.security.SecurityContextOverrideHandler) {
            return SECURITY_CONTEXT_OVERRIDE_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.RestInitialHandler) {
            return REST_INITIAL_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler) {
            return CLASS_ROUTING_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.AbortChainHandler) {
            return ABORT_CHAIN_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.NonBlockingHandler) {
            return NON_BLOCKING_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.BlockingHandler) {
            return BLOCKING_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler) {
            return RESOURCE_REQUEST_FILTER_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InputHandler) {
            return INPUT_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler) {
            return REQUEST_DESERIALIZE_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ParameterHandler) {
            return PARAMETER_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InstanceHandler) {
            return INSTANCE_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.InvocationHandler) {
            return INVOCATION_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.FixedProducesHandler) {
            return FIXED_PRODUCES_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResponseHandler) {
            return RESPONSE_HANDLER;
        } else if (handler instanceof org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler) {
            return RESPONSE_WRITER_HANDLER;
        } else {
            return -1;
        }
    }
}
