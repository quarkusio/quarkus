package io.quarkus.smallrye.openapi.runtime;

import java.util.List;

import io.quarkus.arc.Arc;
import io.smallrye.openapi.runtime.io.Format;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that serve the OpenAPI document in either json or yaml format
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    private volatile OpenApiDocumentService openApiDocumentService;
    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String QUERY_PARAM_FORMAT = "format";

    public OpenApiHandler() {
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest req = event.request();
        HttpServerResponse resp = event.response();

        if (req.method().equals(HttpMethod.OPTIONS)) {
            resp.headers().set("Allow", ALLOWED_METHODS);
            event.next();
        } else {

            // Default content type is YAML
            Format format = Format.YAML;

            String path = event.normalizedPath();
            // Content negotiation with file extension
            if (path.endsWith(".json")) {
                format = Format.JSON;
            } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
                format = Format.YAML;
            } else {
                // Content negotiation with Accept header
                String accept = req.headers().get("Accept");

                List<String> formatParams = event.queryParam(QUERY_PARAM_FORMAT);
                String formatParam = formatParams.isEmpty() ? null : formatParams.get(0);

                // Check Accept, then query parameter "format" for JSON; else use YAML.
                if ((accept != null && accept.contains(Format.JSON.getMimeType()))
                        || ("JSON".equalsIgnoreCase(formatParam))) {
                    format = Format.JSON;
                }
            }

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
