package io.quarkus.devtools.project.update;

public class QuarkusUpdateException extends Exception {

    private static final long serialVersionUID = 1L;

    public QuarkusUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuarkusUpdateException(String message) {
        super(message);
    }
}
