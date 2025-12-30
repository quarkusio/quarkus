package io.quarkus.oidc.common;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
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

    class OidcRequestFilterContext extends OidcRequestContext {
        public OidcRequestFilterContext(HttpRequest<Buffer> request, Buffer requestBody,
                OidcRequestContextProperties contextProperties) {
            super(request, requestBody, contextProperties);
        }

        public final Uni<Void> runBlocking(Runnable runnable) {
            return OidcCommonUtils.runBlocking(runnable);
        }
    }

    /**
     * Filter OIDC request.
     *
     * @param requestContext the request context which provides access to the HTTP request headers and body, as well as context
     *        properties.
     * @deprecated use the {@link #filter(OidcRequestContext)} method instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default void filter(OidcRequestContext requestContext) {
        throw new UnsupportedOperationException("filter(OidcRequestContext requestContext) method is not implemented");
    }

    /**
     * Filter OIDC request asynchronously.
     * Blocking tasks can be run with the {@link OidcRequestFilterContext#runBlocking(Runnable)} method.
     *
     * @param requestContext the request context which provides access to the HTTP request headers and body, as well
     *        as context properties.
     * @return {@link Uni}; must not be null
     */
    default Uni<Void> filter(OidcRequestFilterContext requestContext) {
        return Uni.createFrom().item(() -> {
            filter((OidcRequestContext) requestContext);
            return null;
        });
    }
}
