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
     * OIDC request context which provides access to the HTTP request headers and body, as well as context properties.
     */
    record OidcRequestContext(HttpRequest<Buffer> request, Buffer requestBody, OidcRequestContextProperties contextProperties) {
    }

    /**
     * Filter OIDC request.
     *
     * @param requestContext the request context which provides access to the HTTP request headers and body, as well as context
     *        properties.
     *
     */
    default void filter(OidcRequestContext requestContext) {
        filter(requestContext.request(), requestContext.requestBody(), requestContext.contextProperties());
    }

    /**
     * Filter OIDC requests
     *
     * @param request HTTP request that can have its headers customized
     * @param requestBody request body, will be null for HTTP GET methods, may be null for other HTTP methods
     * @param contextProperties context properties that can be available in context of some requests
     *
     * @deprecated use {@link #filter(OidcRequestContext)}
     */
    @Deprecated(forRemoval = true)
    default void filter(HttpRequest<Buffer> request, Buffer requestBody, OidcRequestContextProperties contextProperties) {
        throw new UnsupportedOperationException(
                "filter(HttpRequest<Buffer> request, Buffer requestBody, OidcRequestContextProperties contextProperties)"
                        + " method is not implemented");
    }
}
