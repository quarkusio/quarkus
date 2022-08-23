package io.quarkus.kafka.client.runtime.ui;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractHttpRequestHandler implements Handler<RoutingContext> {
    private final ManagedContext currentManagedContext;
    private final Handler currentManagedContextTerminationHandler;

    public AbstractHttpRequestHandler() {
        this.currentManagedContext = Arc.container().requestContext();
        this.currentManagedContextTerminationHandler = e -> currentManagedContext.terminate();
    }

    @Override
    @SuppressWarnings("unchecked") // ignore currentManagedContextTerminationHandler types, just use Object
    public void handle(final RoutingContext ctx) {

        if (currentManagedContext.isActive()) {
            doHandle(ctx);
        } else {

            currentManagedContext.activate();
            ctx.response()
                    .endHandler(currentManagedContextTerminationHandler)
                    .exceptionHandler(currentManagedContextTerminationHandler)
                    .closeHandler(currentManagedContextTerminationHandler);

            try {
                doHandle(ctx);
            } catch (Throwable t) {
                currentManagedContext.terminate();
                throw t;
            }
        }
    }

    public void doHandle(RoutingContext ctx) {
        try {
            HttpServerRequest request = ctx.request();

            switch (request.method().name()) {
                case "OPTIONS":
                    handleOptions(ctx);
                    break;
                case "POST":
                    handlePost(ctx);
                    break;
                case "GET":
                    handleGet(ctx);
                    break;
                default:
                    ctx.next();
                    break;
            }
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    public abstract void handlePost(RoutingContext event);

    public abstract void handleGet(RoutingContext event);

    public abstract void handleOptions(RoutingContext event);

    protected String getRequestPath(RoutingContext event) {
        HttpServerRequest request = event.request();
        return request.path();
    }

    //TODO: service methods for HTTP requests
}
