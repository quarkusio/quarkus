package org.jboss.resteasy.reactive.client.handlers;

import jakarta.ws.rs.core.Response;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;

/**
 * This handler is invoked when target server returns an HTTP status of family redirection.
 * <p>
 * Also see {@link RedirectHandler} for a simpler interface that provides fewer options, but handles the most common cases.
 */
public interface AdvancedRedirectHandler {

    /**
     * Allows code to set every aspect of the redirect request
     */
    RequestOptions handle(Context context);

    record Context(Response jaxRsResponse, HttpClientRequest request) {

    }
}
