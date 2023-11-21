package io.quarkus.oidc.common;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

/**
 * Request filter which can be used to customize requests such as the verification JsonWebKey set and token grant requests
 * which are made from the OIDC adapter to the OIDC provider.
 * <p/>
 * Filter can be restricted to a specific OIDC endpoint with a {@link OidcEndpoint} annotation.
 */
public interface OidcRequestFilter {

    /**
     * Filter OIDC requests
     *
     * @param request HTTP request that can have its headers customized
     * @param body request body, will be null for HTTP GET methods, may be null for other HTTP methods
     * @param contextProperties context properties that can be available in context of some requests
     */
    void filter(HttpRequest<Buffer> request, Buffer requestBody, OidcRequestContextProperties contextProperties);
}
