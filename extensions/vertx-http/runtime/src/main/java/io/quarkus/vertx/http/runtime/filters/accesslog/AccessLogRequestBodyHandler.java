package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.util.Locale;
import java.util.Set;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public final class AccessLogRequestBodyHandler implements Handler<RoutingContext> {

    private static final Set<HttpMethod> CAN_HAVE_BODY = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
            HttpMethod.DELETE);

    private final Handler<RoutingContext> bodyHandler;
    private final long bodyBufferLimit;

    public AccessLogRequestBodyHandler(Handler<RoutingContext> bodyHandler, long bodyBufferLimit) {
        this.bodyHandler = bodyHandler;
        this.bodyBufferLimit = bodyBufferLimit;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (isGrpc(ctx) || !CAN_HAVE_BODY.contains(ctx.request().method())) {
            ctx.next();
            return;
        }

        String contentLengthHeader = ctx.request().getHeader("Content-Length");
        if (contentLengthHeader != null) {
            long contentLength = Long.parseLong(contentLengthHeader);
            if (contentLength > bodyBufferLimit) {
                logWithoutBuffering(ctx, contentLength);
                return;
            }
        }

        bodyHandler.handle(ctx);
    }

    private void logWithoutBuffering(RoutingContext ctx, long contentLength) {
        ctx.put(AccessLogBodySupport.REQUEST_BODY_KEY,
                AccessLogBodySupport.formatRequestBodyTooLarge(contentLength));
        ctx.request().handler(buffer -> {
            // discard
        });
        ctx.request().endHandler(v -> ctx.next());
    }

    private static boolean isGrpc(RoutingContext event) {
        String header = event.request().getHeader("content-type");
        return header != null && header.toLowerCase(Locale.ROOT).startsWith("application/grpc");
    }
}
