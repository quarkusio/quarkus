package org.jboss.resteasy.reactive.server.vertx.test.resteasy.async.filters;

@SuppressWarnings("serial")
public class AsyncFilterException extends RuntimeException {

    public AsyncFilterException(final String message) {
        super(message);
    }

}
