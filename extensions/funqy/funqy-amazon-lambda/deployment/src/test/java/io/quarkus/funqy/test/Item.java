package io.quarkus.funqy.test;

public class Item {

    String message;

    boolean throwError;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public boolean isThrowError() {
        return throwError;
    }

    public void setThrowError(final boolean throwError) {
        this.throwError = throwError;
    }
}
