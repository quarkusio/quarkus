package io.quarkus.rest.client.reactive.runtime;

import java.net.URI;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Registered on a CDI client that has no configured base URL but declares an {@code @Url} parameter. The client is
 * built with a placeholder base URI; a request that still points at it, because the {@code @Url} argument was
 * {@code null} or the method has no such parameter, is rejected before it reaches the network.
 */
public class MissingBaseUrlRequestFilter implements ClientRequestFilter {

    static final String PLACEHOLDER_HOST = "base-url-not-configured.invalid";

    static final URI PLACEHOLDER_URI = URI.create("http://" + PLACEHOLDER_HOST);

    private final String message;

    public MissingBaseUrlRequestFilter(String message) {
        this.message = message;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        URI uri = requestContext.getUri();
        if (uri != null && PLACEHOLDER_HOST.equals(uri.getHost())) {
            throw new IllegalStateException(message
                    + ". The interface declares an @Url parameter, so a non-null URL must be passed to each call instead.");
        }
    }
}
