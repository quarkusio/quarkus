package io.quarkus.devtools.project.update.rewrite;

public class QuarkusUpdateExitErrorException extends QuarkusUpdateException {

    private static final long serialVersionUID = 1L;

    public QuarkusUpdateExitErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuarkusUpdateExitErrorException(String message) {
        super(message);
    }
}
