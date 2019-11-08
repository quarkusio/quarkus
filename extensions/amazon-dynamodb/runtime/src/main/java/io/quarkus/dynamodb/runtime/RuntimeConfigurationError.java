package io.quarkus.dynamodb.runtime;

public class RuntimeConfigurationError extends RuntimeException {

    public RuntimeConfigurationError(String message) {
        super(message);
    }
}
