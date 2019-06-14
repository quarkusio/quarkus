package io.quarkus.bootstrap;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapDependencyProcessingException extends BootstrapException {

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
