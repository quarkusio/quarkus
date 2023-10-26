package io.quarkus.oidc.common;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

/**
 * Request filter which can be used to customize OIDC client requests
 */
public interface OidcClientRequestFilter {
    /**
     * Filter OIDC client requests
     *
     * @param request HTTP request
     * @param body request body, will be null for HTTP GET methods, may be null for other HTTP methods
     */
    void filter(HttpRequest<Buffer> request, Buffer body);
}
