package io.quarkus.grpc.runtime.supports.exc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.quarkus.grpc.auth.AuthExceptionHandlerProvider;

@ApplicationScoped
@DefaultBean
public class DefaultExceptionHandlerProvider implements ExceptionHandlerProvider {

    private final AuthExceptionHandlerProvider authExceptionHandlerProvider;

    public DefaultExceptionHandlerProvider(Instance<AuthExceptionHandlerProvider> authExceptionHandlerProviderInstance) {
        if (authExceptionHandlerProviderInstance.isResolvable()) {
            this.authExceptionHandlerProvider = authExceptionHandlerProviderInstance.get();
        } else {
            this.authExceptionHandlerProvider = null;
        }
    }

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> call, Metadata metadata) {
        return new DefaultExceptionHandler<>(listener, call, metadata, authExceptionHandlerProvider);
    }

    @Override
    public Throwable transform(Throwable t) {
        return toStatusException(authExceptionHandlerProvider, t);
    }

    private static Throwable toStatusException(AuthExceptionHandlerProvider authExceptionHandlerProvider, Throwable t) {
        if (authExceptionHandlerProvider != null && authExceptionHandlerProvider.handlesException(t)) {
            return authExceptionHandlerProvider.transformToStatusException(t);
        } else {
            return ExceptionHandlerProvider.toStatusException(t, false);
        }
    }

    private static class DefaultExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {

        private final AuthExceptionHandlerProvider authExceptionHandlerProvider;

        public DefaultExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call,
                Metadata metadata, AuthExceptionHandlerProvider authExceptionHandlerProvider) {
            super(listener, call, metadata);
            this.authExceptionHandlerProvider = authExceptionHandlerProvider;
        }

        @Override
        protected void handleException(Throwable exception, ServerCall<ReqT, RespT> call, Metadata metadata) {
            StatusException se = (StatusException) toStatusException(authExceptionHandlerProvider, exception);
            Metadata trailers = se.getTrailers() != null ? se.getTrailers() : metadata;
            call.close(se.getStatus(), trailers);
        }
    }
}
