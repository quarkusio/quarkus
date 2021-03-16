package io.quarkus.devtools.commands.data;

public class QuarkusCommandException extends Exception {

    private static final long serialVersionUID = 1L;

    public QuarkusCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuarkusCommandException(String message) {
        super(message);
    }
}
