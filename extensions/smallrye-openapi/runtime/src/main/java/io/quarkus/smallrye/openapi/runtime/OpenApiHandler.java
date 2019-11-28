package io.quarkus.smallrye.openapi.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Ken Finnigan
 */
public class OpenApiHandler implements Handler<RoutingContext> {

    /*
     * <em>Ugly Hack</em>
     * In dev mode, we receive a classloader to load the up to date OpenAPI document.
     * This hack is required because using the TCCL would get an outdated version - the initial one.
     * This is because the worker thread on which the handler is called captures the TCCL at creation time
     * and does not allow updating it.
     *
     * This classloader must ONLY be used to load the OpenAPI document.
     *
     * In non dev mode, the TCCL is used.
     *
     * TODO: remove this once the vert.x class loader issues are resolved
     */
    public static volatile ClassLoader classLoader;

    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";

    private static final String QUERY_PARAM_FORMAT = "format";

    public static final String GENERATED_DOC_BASE = "quarkus-generated-openapi-doc.";
    public static final String BASE_NAME = "META-INF/" + GENERATED_DOC_BASE;

    private static void addCorsResponseHeaders(HttpServerResponse response) {
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Credentials", "true");
        response.headers().set("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.headers().set("Access-Control-Max-Age", "86400");
    }

    @Override
    public void handle(RoutingContext event) {
        if (event.request().method().equals(HttpMethod.OPTIONS)) {
            addCorsResponseHeaders(event.response());
            event.response().headers().set("Allow", ALLOWED_METHODS);
        } else {
            HttpServerRequest req = event.request();
            HttpServerResponse resp = event.response();
            String accept = req.headers().get("Accept");

            List<String> formatParams = event.queryParam(QUERY_PARAM_FORMAT);
            String formatParam = formatParams.isEmpty() ? null : formatParams.get(0);

            // Default content type is YAML
            OpenApiSerializer.Format format = OpenApiSerializer.Format.YAML;

            // Check Accept, then query parameter "format" for JSON; else use YAML.
            if ((accept != null && accept.contains(OpenApiSerializer.Format.JSON.getMimeType())) ||
                    ("JSON".equalsIgnoreCase(formatParam))) {
                format = OpenApiSerializer.Format.JSON;
            }

            addCorsResponseHeaders(resp);
            resp.headers().set("Content-Type", format.getMimeType() + ";charset=UTF-8");
            ClassLoader cl = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
            try (InputStream in = cl.getResourceAsStream(BASE_NAME + format)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int r;
                byte[] buf = new byte[1024];
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
                resp.end(Buffer.buffer(out.toByteArray()));
            } catch (IOException e) {
                event.fail(e);
            }

        }
    }
}
