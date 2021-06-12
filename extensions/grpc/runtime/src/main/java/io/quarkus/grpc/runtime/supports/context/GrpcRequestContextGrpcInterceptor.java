package io.quarkus.grpc.runtime.supports.context;

import org.jboss.logmanager.Logger;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class GrpcRequestContextGrpcInterceptor implements ServerInterceptor {

    private final ManagedContext reqContext;
    private static final Logger LOGGER = Logger.getLogger(GrpcRequestContextGrpcInterceptor.class.getName());

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
            GrpcRequestContextHolder contextHolder = GrpcRequestContextHolder.initialize(capturedVertxContext);
            ServerCall.Listener<ReqT> delegate = next
                    .startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

                        @Override
                        public void close(Status status, Metadata trailers) {
                            super.close(status, trailers);
                            if (contextHolder.state != null) {
                                reqContext.destroy(contextHolder.state);
                                reqContext.deactivate();
                            }
                        }
                    }, headers);

            // a gRPC service can return a StreamObserver<Messages.StreamingInputCallRequest> and instead of doing the work
            // directly in the method body, do stuff that requires a request context in StreamObserver's methods
            // let's propagate the request context to these methods:
            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {

                @Override
                public void onMessage(ReqT message) {
                    boolean activated = activateContext();
                    try {
                        super.onMessage(message);
                    } finally {
                        if (activated) {
                            reqContext.deactivate();
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
                            reqContext.deactivate();
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
                            reqContext.deactivate();
                        }
                    }
                }

                @Override
                public void onCancel() {
                    boolean activated = activateContext();
                    try {
                        super.onHalfClose();
                    } finally {
                        if (activated) {
                            reqContext.deactivate();
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
                            reqContext.deactivate();
                        }
                    }
                }

                private boolean activateContext() {
                    if (contextHolder.state != null && !reqContext.isActive()) {
                        reqContext.activate(contextHolder.state);
                        return true;
                    }
                    return false;
                }
            };
        } else {
            LOGGER.warning("Unable to activate the request scope - interceptor not called on the Vert.x event loop");
            return next.startCall(call, headers);
        }
    }
}
