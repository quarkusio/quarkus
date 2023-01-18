package io.quarkus.grpc.runtime.supports.exc;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;

@ApplicationScoped
@DefaultBean
public class DefaultExceptionHandlerProvider implements ExceptionHandlerProvider {
    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> call, Metadata metadata) {
        return new DefaultExceptionHandler<>(listener, call, metadata);
    }

    private static class DefaultExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {
        public DefaultExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call,
                Metadata metadata) {
            super(listener, call, metadata);
        }

        @Override
        protected void handleException(Throwable exception, ServerCall<ReqT, RespT> call, Metadata metadata) {
            StatusException se = (StatusException) ExceptionHandlerProvider.toStatusException(exception, false);
            Metadata trailers = se.getTrailers() != null ? se.getTrailers() : metadata;
            call.close(se.getStatus(), trailers);
        }
    }
}
