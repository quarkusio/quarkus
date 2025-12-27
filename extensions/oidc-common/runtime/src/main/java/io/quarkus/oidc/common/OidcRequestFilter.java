package io.quarkus.oidc.common;

import io.smallrye.mutiny.Uni;
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
    class OidcRequestContext {
        final HttpRequest<Buffer> request;
        final OidcRequestContextProperties contextProperties;
        Buffer requestBody;

        public OidcRequestContext(HttpRequest<Buffer> request, Buffer requestBody,
                OidcRequestContextProperties contextProperties) {
            this.request = request;
            this.requestBody = requestBody;
            this.contextProperties = contextProperties;
        }

        public HttpRequest<Buffer> request() {
            return request;
        }

        public Buffer requestBody() {
            return requestBody;
        }

        public OidcRequestContextProperties contextProperties() {
            return contextProperties;
        }

        public void requestBody(Buffer buffer) {
            requestBody = buffer;
            contextProperties.put(OidcRequestContextProperties.REQUEST_BODY, buffer);
        }

    }

    /**
     * An {@link OidcRequestContext} implementation used for asynchronous filtering.
     */
    final class OidcRequestFilterContext extends OidcRequestContext {

        public OidcRequestFilterContext(HttpRequest<Buffer> request, Buffer requestBody,
                OidcRequestContextProperties contextProperties) {
            super(request, requestBody, contextProperties);
        }
    }

    /**
     * Filter OIDC request.
     *
     * <b>IMPORTANT:</b> If you need to block or filter this request asynchronously,
     * implement the {@link #filter(OidcRequestFilterContext)} method instead.
     *
     * @param requestContext the request context which provides access to the HTTP request headers and body, as well as context
     *        properties.
     *
     */
    default void filter(OidcRequestContext requestContext) {
        filter(requestContext.request(), requestContext.requestBody(), requestContext.contextProperties());
    }

    /**
     * Filter OIDC request asynchronously.
     *
     * @param requestContext the request context which provides access to the HTTP request headers and body, as well as context
     *        properties.
     * @return {@link Uni}; must not be null
     */
    default Uni<Void> filter(OidcRequestFilterContext requestContext) {
        return Uni.createFrom().item(() -> {
            filter((OidcRequestContext) requestContext);
            return null;
        });
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
