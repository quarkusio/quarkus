package io.quarkus.grpc.auth;

import jakarta.inject.Singleton;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.AuthenticationException;

@Singleton
public class DefaultAuthExceptionHandlerProvider implements AuthExceptionHandlerProvider {

    private final boolean addStatusDescription;

    public DefaultAuthExceptionHandlerProvider() {
        this.addStatusDescription = LaunchMode.current().isDevOrTest();
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public <ReqT, RespT> AuthExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        return new AuthExceptionHandler<>(listener, serverCall, metadata, addStatusDescription);
    }

    @Override
    public StatusException transformToStatusException(Throwable t) {
        return new StatusException(transformToStatusException(addStatusDescription, t));
    }

    @Override
    public boolean handlesException(Throwable failure) {
        return failure instanceof AuthenticationException || failure instanceof SecurityException;
    }

    static Status transformToStatusException(boolean addExceptionMessage, Throwable exception) {
        if (exception instanceof AuthenticationException) {
            if (addExceptionMessage) {
                return Status.UNAUTHENTICATED.withDescription(exception.getMessage());
            } else {
                return Status.UNAUTHENTICATED;
            }
        } else if (exception instanceof SecurityException) {
            if (addExceptionMessage) {
                return Status.PERMISSION_DENIED.withDescription(exception.getMessage());
            } else {
                return Status.PERMISSION_DENIED;
            }
        } else {
            throw new IllegalStateException("Cannot transform exception " + exception, exception);
        }
    }
}
