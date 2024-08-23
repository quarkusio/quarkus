package io.quarkus.vertx.web.runtime;

import java.util.Set;

import io.quarkus.vertx.http.runtime.HttpCompression;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class HttpCompressionHandler implements Handler<RoutingContext> {

    private final Handler<RoutingContext> routeHandler;
    private final HttpCompression compression;
    private final Set<String> compressedMediaTypes;

    public HttpCompressionHandler(Handler<RoutingContext> routeHandler, HttpCompression compression,
            Set<String> compressedMediaTypes) {
        this.routeHandler = routeHandler;
        this.compression = compression;
        this.compressedMediaTypes = compressedMediaTypes;
    }

    @Override
    public void handle(RoutingContext context) {
        context.addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void result) {
                MultiMap headers = context.response().headers();
                String contentEncoding = headers.get(HttpHeaders.CONTENT_ENCODING);
                if (contentEncoding != null && HttpHeaders.IDENTITY.toString().equals(contentEncoding)) {
                    switch (compression) {
                        case ON:
                            headers.remove(HttpHeaders.CONTENT_ENCODING);
                            break;
                        case UNDEFINED:
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
                            break;
                        default:
                            // OFF - no action is needed because the "Content-Encoding: identity" header is set
                            break;
                    }
                }
            }
        });
        routeHandler.handle(context);
    }

}
