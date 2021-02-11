package io.quarkus.funqy.runtime;

public class ApplicationException extends RuntimeException {
    public ApplicationException(Throwable cause) {
        super(cause);
    }
}
