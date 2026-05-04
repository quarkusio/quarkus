package org.jboss.resteasy.reactive.client.impl;

import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;

import java.net.URI;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.HttpUtils;

/**
 * Reimplements Vert.x default redirect handling so Quarkus can make the same redirect decision before Vert.x creates the
 * redirected physical request.
 *
 * @see io.vertx.core.http.impl.HttpClientImpl#DEFAULT_HANDLER
 *
 *      This is needed because request customizers are applied in Quarkus, not in Vert.x. If Quarkus does not have an
 *      explicit redirect function for the default-follow-redirects case, it cannot get hold of the redirected
 *      {@link io.vertx.core.http.HttpClientRequest} early enough to reapply those customizers.
 */
class DefaultVertxRedirectHandlerImpl implements Function<HttpClientResponse, Future<RequestOptions>> {

    @Override
    public Future<RequestOptions> apply(HttpClientResponse resp) {
        try {
            int statusCode = resp.statusCode();
            String location = resp.getHeader(HttpHeaders.LOCATION);
            if (location != null && (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307
                    || statusCode == 308)) {
                HttpMethod m = resp.request().getMethod();
                if (statusCode == 303) {
                    m = HttpMethod.GET;
                } else if (m != HttpMethod.GET && m != HttpMethod.HEAD) {
                    return null;
                }
                URI uri = HttpUtils.resolveURIReference(resp.request().absoluteURI(), location);
                boolean ssl;
                int port = uri.getPort();
                String protocol = uri.getScheme();
                char chend = protocol.charAt(protocol.length() - 1);
                if (chend == 'p') {
                    ssl = false;
                    if (port == -1) {
                        port = 80;
                    }
                } else if (chend == 's') {
                    ssl = true;
                    if (port == -1) {
                        port = 443;
                    }
                } else {
                    return null;
                }
                String requestURI = uri.getPath();
                if (requestURI == null || requestURI.isEmpty()) {
                    requestURI = "/";
                }
                String query = uri.getQuery();
                if (query != null) {
                    requestURI += "?" + query;
                }
                RequestOptions options = new RequestOptions();
                options.setMethod(m);
                options.setHost(uri.getHost());
                options.setPort(port);
                options.setSsl(ssl);
                options.setURI(requestURI);
                options.setHeaders(resp.request().headers());
                options.removeHeader(CONTENT_LENGTH);
                return Future.succeededFuture(options);
            }
            return null;
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }
}
