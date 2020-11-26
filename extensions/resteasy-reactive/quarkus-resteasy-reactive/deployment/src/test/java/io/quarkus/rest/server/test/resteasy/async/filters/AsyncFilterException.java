package io.quarkus.rest.server.test.resteasy.async.filters;

@SuppressWarnings("serial")
public class AsyncFilterException extends RuntimeException {

    public AsyncFilterException(final String message) {
        super(message);
    }

}
