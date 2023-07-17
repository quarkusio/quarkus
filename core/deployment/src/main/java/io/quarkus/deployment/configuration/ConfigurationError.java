package io.quarkus.deployment.configuration;

@Deprecated
public class ConfigurationError extends RuntimeException {
    public ConfigurationError(final String message) {
        super(message);
    }
}
