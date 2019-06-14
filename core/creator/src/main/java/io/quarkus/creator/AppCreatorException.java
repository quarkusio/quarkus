package io.quarkus.creator;

/**
 * Main application creator exception.
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AppCreatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppCreatorException(String message) {
        super(message);
    }
}
