package org.jboss.resteasy.reactive.client.handlers;

import java.net.URI;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;

/**
 * This handler is invoked when target server returns an HTTP status of family redirection.
 * <p>
 * Also see {@link AdvancedRedirectHandler} if more control is needed.
 */
public interface RedirectHandler {
    int DEFAULT_PRIORITY = 5000;

    URI handle(Response response);

    default int getPriority() {
        return Optional.ofNullable(this.getClass().getAnnotation(Priority.class)).map(Priority::value).orElse(DEFAULT_PRIORITY);
    }
}
