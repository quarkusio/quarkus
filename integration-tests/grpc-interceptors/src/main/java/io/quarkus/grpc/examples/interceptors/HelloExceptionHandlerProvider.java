package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;

@ApplicationScoped
public class HelloExceptionHandlerProvider implements ExceptionHandlerProvider {
    public static boolean invoked;

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        return new HelloExceptionHandler<>(listener, serverCall, metadata);
    }

    @Override
    public Throwable transform(Throwable t) {
        invoked = true;
        if (t instanceof HelloException) {
            HelloException he = (HelloException) t;
            return new StatusRuntimeException(Status.ABORTED.withDescription(he.getName()));
        } else {
            return ExceptionHandlerProvider.toStatusException(t, true);
        }
    }

    private static class HelloExceptionHandler<A, B> extends ExceptionHandler<A, B> {
        public HelloExceptionHandler(ServerCall.Listener<A> listener, ServerCall<A, B> call, Metadata metadata) {
            super(listener, call, metadata);
        }

        @Override
        protected void handleException(Throwable t, ServerCall<A, B> call, Metadata metadata) {
            invoked = true;
            StatusRuntimeException sre = (StatusRuntimeException) ExceptionHandlerProvider.toStatusException(t, true);
            Metadata trailers = sre.getTrailers() != null ? sre.getTrailers() : metadata;
            call.close(sre.getStatus(), trailers);
        }
    }
}
