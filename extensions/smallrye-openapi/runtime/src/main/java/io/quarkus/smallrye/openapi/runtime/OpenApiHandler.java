package io.quarkus.smallrye.openapi.runtime;

import java.util.List;

import io.quarkus.arc.Arc;
import io.smallrye.openapi.runtime.io.Format;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that serve the OpenAPI document in either json or yaml format
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    private volatile OpenApiDocumentService openApiDocumentService;
    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String QUERY_PARAM_FORMAT = "format";
    private static final MultiMap RESPONSE_HEADERS = new HeadersMultiMap();

    static {
        RESPONSE_HEADERS.add("access-control-allow-origin", "*");
        RESPONSE_HEADERS.add("access-control-allow-credentials", "true");
        RESPONSE_HEADERS.add("access-control-allow-methods", ALLOWED_METHODS);
        RESPONSE_HEADERS.add("access-control-allow-headers", "Content-Type, Authorization");
        RESPONSE_HEADERS.add("access-control-max-age", "86400");
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest req = event.request();
        HttpServerResponse resp = event.response();

        if (req.method().equals(HttpMethod.OPTIONS)) {
            resp.headers().setAll(RESPONSE_HEADERS);
            resp.headers().set("Allow", ALLOWED_METHODS);
            event.next();
        } else {
            String accept = req.headers().get("Accept");

            List<String> formatParams = event.queryParam(QUERY_PARAM_FORMAT);
            String formatParam = formatParams.isEmpty() ? null : formatParams.get(0);

            // Default content type is YAML
            Format format = Format.YAML;

            // Check Accept, then query parameter "format" for JSON; else use YAML.
            if ((accept != null && accept.contains(Format.JSON.getMimeType())) ||
                    ("JSON".equalsIgnoreCase(formatParam))) {
                format = Format.JSON;
            }

            resp.headers().setAll(RESPONSE_HEADERS);
            resp.headers().set("Content-Type", format.getMimeType() + ";charset=UTF-8");
            byte[] schemaDocument = getOpenApiDocumentService().getDocument(format);
            resp.end(Buffer.buffer(schemaDocument));
        }
    }

    private OpenApiDocumentService getOpenApiDocumentService() {
        if (this.openApiDocumentService == null) {
            this.openApiDocumentService = Arc.container().instance(OpenApiDocumentService.class).get();
        }
        return this.openApiDocumentService;
    }
}
