package io.quarkus.vertx.http.runtime;

import java.util.Set;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * A simple wrapping handler that removes the {@code Content-Encoding: identity} HTTP header if the {@code Content-Type}
 * header is set and the value is a compressed media type as configured via
 * {@link VertxHttpBuildTimeConfig#compressMediaTypes}.
 */
public class HttpCompressionHandler implements Handler<RoutingContext> {

    private final Handler<RoutingContext> routeHandler;
    private final Set<String> compressedMediaTypes;

    public HttpCompressionHandler(Handler<RoutingContext> routeHandler, Set<String> compressedMediaTypes) {
        this.routeHandler = routeHandler;
        this.compressedMediaTypes = compressedMediaTypes;
    }

    @Override
    public void handle(RoutingContext context) {
        context.addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void result) {
                compressIfNeeded(context, compressedMediaTypes);
            }
        });
        routeHandler.handle(context);
    }

    public static void compressIfNeeded(RoutingContext context, Set<String> compressedMediaTypes) {
        MultiMap headers = context.response().headers();
        // "Content-Encoding: identity" header means that compression is enabled in the config
        // and this header is set to disable the compression by default
        String contentEncoding = headers.get(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && HttpHeaders.IDENTITY.toString().equals(contentEncoding)) {
            // Just remove the header if the compression should be enabled for the current HTTP response
            String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
            if (contentType != null) {
                int paramIndex = contentType.indexOf(';');
                if (paramIndex > -1) {
                    contentType = contentType.substring(0, paramIndex);
                }
                if (compressedMediaTypes.contains(contentType)) {
                    headers.remove(HttpHeaders.CONTENT_ENCODING);
                }
            }
        }
    }

}
