package io.quarkus.bootstrap;

public class BootstrapException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapException(String message) {
        super(message);
    }
}
