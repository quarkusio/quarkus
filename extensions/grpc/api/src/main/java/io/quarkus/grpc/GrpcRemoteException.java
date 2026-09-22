package io.quarkus.grpc;

/**
 * Represents an exception thrown by a remote gRPC service.
 * Instances are typically created on the client when restoring an exception cause chain from response trailers.
 */
public class GrpcRemoteException extends Exception {

    private final String exceptionClassName;

    public GrpcRemoteException(String exceptionClassName, String message) {
        super(message);
        this.exceptionClassName = exceptionClassName;
    }

    /**
     * @return the fully qualified class name of the original remote exception
     */
    public String getExceptionClassName() {
        return exceptionClassName;
    }

    @Override
    public String toString() {
        String message = getLocalizedMessage();
        return message == null || message.isEmpty()
                ? exceptionClassName
                : exceptionClassName + ": " + message;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
