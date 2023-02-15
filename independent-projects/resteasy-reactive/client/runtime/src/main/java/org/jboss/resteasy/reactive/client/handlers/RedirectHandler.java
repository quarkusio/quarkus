package org.jboss.resteasy.reactive.client.handlers;

import java.net.URI;

import jakarta.ws.rs.core.Response;

/**
 * This handler is invoked when target server returns an HTTP status of family redirection.
 */
public interface RedirectHandler {
    URI handle(Response response);
}
