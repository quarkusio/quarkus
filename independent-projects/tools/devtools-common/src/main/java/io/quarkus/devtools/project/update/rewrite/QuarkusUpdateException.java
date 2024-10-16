package io.quarkus.devtools.project.update.rewrite;

public class QuarkusUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QuarkusUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuarkusUpdateException(String message) {
        super(message);
    }
}
