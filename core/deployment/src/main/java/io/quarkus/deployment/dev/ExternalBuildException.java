package io.quarkus.deployment.dev;

final class ExternalBuildException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ExternalBuildException(String message) {
        super(message == null || message.isBlank() ? "The external build failed" : message);
    }
}
