package io.quarkus.platform.descriptor.resolver.json;

public class VersionNotAvailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public VersionNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionNotAvailableException(String message) {
        super(message);
    }
}
