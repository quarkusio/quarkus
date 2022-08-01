package io.quarkus.registry;

public class RegistryResolutionException extends Exception {

    public RegistryResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryResolutionException(String message) {
        super(message);
    }
}
