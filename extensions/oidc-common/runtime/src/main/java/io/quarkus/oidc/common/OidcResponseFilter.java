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
    class OidcResponseContext {
        final OidcRequestContextProperties requestProperties;
        final int statusCode;
        final MultiMap responseHeaders;
        Buffer responseBody;

        public OidcResponseContext(OidcRequestContextProperties requestProperties, int statusCode,
                MultiMap responseHeaders, Buffer responseBody) {
            this.requestProperties = requestProperties;
            this.statusCode = statusCode;
            this.responseHeaders = responseHeaders;
            this.responseBody = responseBody;
        }

        public OidcRequestContextProperties requestProperties() {
            return requestProperties;
        }

        public int statusCode() {
            return statusCode;
        }

        public MultiMap responseHeaders() {
            return responseHeaders;
        }

        public Buffer responseBody() {
            return responseBody;
        }

        public void responseBody(Buffer buffer) {
            responseBody = buffer;
            requestProperties.put(OidcRequestContextProperties.RESPONSE_BODY, buffer);
        }
    }

    /**
     * Filter OIDC responses.
     *
     * @param responseContext the response context which provides access to the HTTP response status code, headers and body.
     *
     */
    void filter(OidcResponseContext responseContext);
}
