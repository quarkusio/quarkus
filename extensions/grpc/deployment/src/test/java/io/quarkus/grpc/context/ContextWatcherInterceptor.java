package io.quarkus.grpc.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.grpc.runtime.Interceptors;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Passive watcher for gRPC context lifecycle integration tests.
 * <p>
 * Uses two {@code @GlobalInterceptor}s to observe two distinct moments, both positioned
 * <em>after</em> {@code GrpcDuplicatedContextGrpcInterceptor} has acted:
 * <ul>
 * <li>{@link Outer} (MAX_VALUE−6) wraps {@code ServerCall.close()} — which propagates
 * outward through the chain, so {@code super.close()} calls into
 * {@code GrpcDuplicatedContextGrpcInterceptor}'s cleanup before the latch fires.</li>
 * <li>{@link Inner} (priority 0) wraps the listener — as the innermost interceptor it is
 * the last to process {@code onMessage} and {@code onHalfClose} before the service
 * handler on all paths: directly on the event-loop for non-blocking calls, and inside
 * {@code BlockingServerInterceptor}'s {@code ReplayListener} delegate chain for
 * blocking and virtual-thread calls.</li>
 * </ul>
 */
@ApplicationScoped
public class ContextWatcherInterceptor {

    private volatile Context capturedDuplicatedContext;
    private volatile io.grpc.Context contextAtOnMessage;
    private volatile io.grpc.Context contextAtOnHalfClose;
    private volatile CountDownLatch closeLatch;

    /** Reset between tests. */
    public void reset() {
        capturedDuplicatedContext = null;
        contextAtOnMessage = null;
        contextAtOnHalfClose = null;
        closeLatch = new CountDownLatch(1);
    }

    public void setCapturedDuplicatedContext(Context ctx) {
        capturedDuplicatedContext = ctx;
    }

    public void setContextAtOnMessage(io.grpc.Context ctx) {
        contextAtOnMessage = ctx;
    }

    public io.grpc.Context getContextAtOnMessage() {
        return contextAtOnMessage;
    }

    public void setContextAtOnHalfClose(io.grpc.Context ctx) {
        contextAtOnHalfClose = ctx;
    }

    public io.grpc.Context getContextAtOnHalfClose() {
        return contextAtOnHalfClose;
    }

    public void countDownClose() {
        CountDownLatch latch = closeLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    public boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = closeLatch;
        return latch != null && latch.await(timeout, unit);
    }

    /** Reads {@code Context.current()} on the captured duplicated Vert.x context. Call after {@link #awaitClose}. */
    public CompletableFuture<io.grpc.Context> readContextOnDuplicatedContext() {
        CompletableFuture<io.grpc.Context> result = new CompletableFuture<>();
        capturedDuplicatedContext.runOnContext(v -> result.complete(io.grpc.Context.current()));
        return result;
    }

    /**
     * Outer interceptor — runs on the event-loop thread, between
     * {@code GrpcDuplicatedContextGrpcInterceptor} and {@code BlockingServerInterceptor}.
     */
    @ApplicationScoped
    @GlobalInterceptor
    public static class Outer implements ServerInterceptor, Prioritized {

        @Inject
        ContextWatcherInterceptor watcher;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

            watcher.setCapturedDuplicatedContext(Vertx.currentContext());

            ServerCall<ReqT, RespT> forwardingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                @Override
                public void close(Status status, Metadata trailers) {
                    // Cleanup runs inside super.close() (GrpcDuplicatedContextGrpcInterceptor is outer);
                    // latch fires after cleanup is done.
                    try {
                        super.close(status, trailers);
                    } finally {
                        watcher.countDownClose();
                    }
                }
            };

            return next.startCall(forwardingCall, headers);
        }

        @Override
        public int getPriority() {
            return Interceptors.DUPLICATE_CONTEXT - 1;
        }
    }

    /** Innermost interceptor — direct delegate of {@code BlockingServerInterceptor}'s {@code ReplayListener}. */
    @ApplicationScoped
    @GlobalInterceptor
    public static class Inner implements ServerInterceptor, Prioritized {

        @Inject
        ContextWatcherInterceptor watcher;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
                    next.startCall(call, headers)) {

                @Override
                public void onMessage(ReqT message) {
                    watcher.setContextAtOnMessage(io.grpc.Context.current());
                    super.onMessage(message);
                }

                @Override
                public void onHalfClose() {
                    watcher.setContextAtOnHalfClose(io.grpc.Context.current());
                    super.onHalfClose();
                }
            };
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }
}
