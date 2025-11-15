package io.quarkus.azure.functions.deployment;

public class DeploymentException extends RuntimeException {

    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
