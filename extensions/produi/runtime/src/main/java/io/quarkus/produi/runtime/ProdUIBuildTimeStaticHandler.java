package io.quarkus.produi.runtime;

import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Serves build-time-generated static content (index.html, *-data.js files)
 * for the Prod UI.
 */
public class ProdUIBuildTimeStaticHandler implements Handler<RoutingContext> {

    private final Map<String, byte[]> content;

    public ProdUIBuildTimeStaticHandler(Map<String, byte[]> content) {
        this.content = content;
    }

    @Override
    public void handle(RoutingContext event) {
        String path = event.normalizedPath();

        for (Map.Entry<String, byte[]> entry : content.entrySet()) {
            if (path.endsWith(entry.getKey())) {
                String contentType = guessContentType(entry.getKey());
                event.response()
                        .putHeader("Content-Type", contentType)
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .end(Buffer.buffer(entry.getValue()));
                return;
            }
        }
        event.next();
    }

    private static String guessContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html;charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript;charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            return "text/css;charset=UTF-8";
        } else if (fileName.endsWith(".json")) {
            return "application/json;charset=UTF-8";
        }
        return "application/octet-stream";
    }
}
