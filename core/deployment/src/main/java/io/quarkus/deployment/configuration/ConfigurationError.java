package io.quarkus.deployment.configuration;

public class ConfigurationError extends RuntimeException {
    public ConfigurationError(final String message) {
        super(message);
    }
}
