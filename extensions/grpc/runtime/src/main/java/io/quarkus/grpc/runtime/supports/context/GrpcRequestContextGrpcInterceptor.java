package io.quarkus.grpc.runtime.supports.context;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Prioritized;

import org.jboss.logging.Logger;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class GrpcRequestContextGrpcInterceptor implements ServerInterceptor, Prioritized {
    private static final Logger log = Logger.getLogger(GrpcRequestContextGrpcInterceptor.class.getName());

    private final ManagedContext reqContext;

    public GrpcRequestContextGrpcInterceptor() {
        reqContext = Arc.container().requestContext();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // This interceptor is called first, so, we should be on the event loop.
        Context capturedVertxContext = Vertx.currentContext();
        if (capturedVertxContext != null) {
            InjectableContext.ContextState state;
            if (!reqContext.isActive()) {
                reqContext.activate();
                state = reqContext.getState();
            } else {
                state = null;
                log.warn("Request context already active when gRPC request started");
            }

            // a gRPC service can return a StreamObserver<Messages.StreamingInputCallRequest> and instead of doing the work
            // directly in the method body, do stuff that requires a request context in StreamObserver's methods
            // let's propagate the request context to these methods:
            try {
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                        next.startCall(call, headers)) {

                    @Override
                    public void onMessage(ReqT message) {
                        boolean activated = activateContext();
                        try {
                            super.onMessage(message);
                        } finally {
                            if (activated) {
                                deactivateContext();
                            }
                        }
                    }

                    @Override
                    public void onReady() {
                        boolean activated = activateContext();
                        try {
                            super.onReady();
                        } finally {
                            if (activated) {
                                deactivateContext();
                            }
                        }
                    }

                    @Override
                    public void onHalfClose() {
                        boolean activated = activateContext();
                        try {
                            super.onHalfClose();
                        } finally {
                            if (activated) {
                                deactivateContext();
                            }
                        }
                    }

                    @Override
                    public void onCancel() {
                        boolean activated = activateContext();
                        try {
                            super.onCancel();
                        } finally {
                            if (activated) {
                                deactivateContext();
                            }
                            if (state != null) {
                                reqContext.destroy(state);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        boolean activated = activateContext();
                        try {
                            super.onComplete();
                        } finally {
                            if (activated) {
                                deactivateContext();
                            }
                            if (state != null) {
                                reqContext.destroy(state);
                            }
                        }
                    }

                    private void deactivateContext() {
                        reqContext.deactivate();
                    }

                    private boolean activateContext() {
                        if (state != null && !reqContext.isActive()) {
                            reqContext.activate(state);
                            return true;
                        }
                        return false;
                    }
                };
            } finally {
                reqContext.deactivate();
            }
        } else {
            log.warn("Unable to activate the request scope - interceptor not called on the Vert.x event loop");
            return next.startCall(call, headers);
        }
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
