package io.quarkus.hibernate.search.orm.elasticsearch.runtime.management;

import java.util.Locale;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class HibernateSearchManagementHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            doHandle(routingContext);
        } else {
            requestContext.activate();
            try {
                doHandle(routingContext);
            } finally {
                requestContext.terminate();
            }
        }
    }

    private void doHandle(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();

        if (!HttpMethod.POST.equals(request.method())) {
            errorResponse(ctx, 406, "Http method [" + request.method().name() + "] is not supported. Use [POST] instead.");
            return;
        }

        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            errorResponse(ctx, 406, "Content type [" + contentType + " is not supported. Use [application/json] instead.");
            return;
        }

        new HibernateSearchPostRequestProcessor().process(ctx);
    }

    private void errorResponse(RoutingContext ctx, int code, String message) {
        ctx.response()
                .setStatusCode(code)
                .setStatusMessage(message)
                .end();
    }
}
