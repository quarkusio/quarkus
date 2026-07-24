package io.quarkus.grpc.runtime.supports.exc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.quarkus.arc.DefaultBean;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.quarkus.grpc.auth.AuthExceptionHandlerProvider;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;

@ApplicationScoped
@DefaultBean
public class DefaultExceptionHandlerProvider implements ExceptionHandlerProvider {

    private final AuthExceptionHandlerProvider authExceptionHandlerProvider;
    private final GrpcConfiguration grpcConfiguration;

    public DefaultExceptionHandlerProvider(Instance<AuthExceptionHandlerProvider> authExceptionHandlerProviderInstance,
            GrpcConfiguration grpcConfiguration) {
        if (authExceptionHandlerProviderInstance.isResolvable()) {
            this.authExceptionHandlerProvider = authExceptionHandlerProviderInstance.get();
        } else {
            this.authExceptionHandlerProvider = null;
        }
        this.grpcConfiguration = grpcConfiguration;
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

    private Throwable toStatusException(AuthExceptionHandlerProvider authExceptionHandlerProvider, Throwable t) {
        if (authExceptionHandlerProvider != null && authExceptionHandlerProvider.handlesException(t)) {
            return authExceptionHandlerProvider.transformToStatusException(t);
        } else {
            return ExceptionHandlerProvider.toStatusException(t, true, grpcConfiguration.server().propagateExceptionCauses());
        }
    }

    private class DefaultExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {

        private final AuthExceptionHandlerProvider authExceptionHandlerProvider;

        public DefaultExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call,
                Metadata metadata, AuthExceptionHandlerProvider authExceptionHandlerProvider) {
            super(listener, call, metadata);
            this.authExceptionHandlerProvider = authExceptionHandlerProvider;
        }

        @Override
        protected void handleException(Throwable exception, ServerCall<ReqT, RespT> call, Metadata metadata) {
            Throwable transformed = toStatusException(authExceptionHandlerProvider, exception);
            Status status = ExceptionHandlerProvider.toStatus(transformed);
            Metadata trailers = ExceptionHandlerProvider.toTrailers(transformed).orElse(metadata);
            try {
                call.close(status, trailers);
            } catch (IllegalStateException ise) {
                // Ignore the exception if the server is already closed
                // Since we don't have a specific exception type for this case, we need to check the message
                if (!"Already closed".equals(ise.getMessage())) {
                    throw ise;
                }
            }
        }
    }
}
