package io.quarkus.rest.data.panache;

public class RestDataPanacheException extends RuntimeException {

    public RestDataPanacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
