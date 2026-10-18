package io.quarkus.bootstrap;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapDependencyProcessingException extends BootstrapMavenException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public BootstrapDependencyProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapDependencyProcessingException(String message) {
        super(message);
    }
}
