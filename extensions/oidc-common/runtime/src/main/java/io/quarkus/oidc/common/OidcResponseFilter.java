package io.quarkus.oidc.common;

import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;

/**
 * Response filter which can be used to intercept HTTP responses from the OIDC provider.
 * <p/>
 * Filter can be restricted to a specific OIDC endpoint with a {@link OidcEndpoint} annotation.
 */
public interface OidcResponseFilter {

    /**
     * OIDC response context which provides access to the HTTP response status code, headers and body.
     */
    record OidcResponseContext(OidcRequestContextProperties requestProperties,
            int statusCode, MultiMap responseHeaders, Buffer responseBody) {
    }

    /**
     * Filter OIDC responses.
     *
     * @param responseContext the response context which provides access to the HTTP response status code, headers and body.
     *
     */
    void filter(OidcResponseContext responseContext);
}
