package io.quarkus.bootstrap.resolver.maven;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInjectionException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DeploymentInjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeploymentInjectionException(Throwable cause) {
        super(cause);
    }

    public DeploymentInjectionException(String message) {
        super(message);
    }
}
