package io.quarkus.oidc.common;

import io.smallrye.mutiny.Uni;
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
     * An {@link OidcResponseContext} implementation used for asynchronous filtering.
     */
    final class OidcResponseFilterContext extends OidcResponseContext {

        public OidcResponseFilterContext(OidcRequestContextProperties requestProperties, int statusCode,
                MultiMap responseHeaders, Buffer responseBody) {
            super(requestProperties, statusCode, responseHeaders, responseBody);
        }
    }

    /**
     * Filter OIDC responses.
     *
     * <b>IMPORTANT:</b> If you need to block or filter this request asynchronously,
     * implement the {@link #filter(OidcResponseFilterContext)} method instead.
     *
     * @param responseContext the response context which provides access to the HTTP response status code, headers and body.
     *
     */
    default void filter(OidcResponseContext responseContext) {
        throw new UnsupportedOperationException("filter(OidcResponseContext responseContext) method is not implemented");
    }

    /**
     * Filter OIDC responses asynchronously.
     *
     * @param responseContext the response context which provides access to the HTTP response status code, headers and body.
     * @return {@link Uni}
     */
    default Uni<Void> filter(OidcResponseFilterContext responseContext) {
        return Uni.createFrom().item(() -> {
            filter((OidcResponseContext) responseContext);
            return null;
        });
    }

}
