package io.quarkus.jfr.it;

public class JfrTestException extends RuntimeException {
    public JfrTestException(String message) {
        super(message);
    }
}
