package io.quarkus.kafka.client.runtime.ui;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractHttpRequestHandler implements Handler<RoutingContext> {
    private final CurrentIdentityAssociation currentIdentityAssociation;
    private final CurrentVertxRequest currentVertxRequest;
    private final ManagedContext currentManagedContext;
    private final Handler currentManagedContextTerminationHandler;

    public AbstractHttpRequestHandler(CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {
        this.currentIdentityAssociation = currentIdentityAssociation;
        this.currentVertxRequest = currentVertxRequest;
        this.currentManagedContext = Arc.container().requestContext();
        this.currentManagedContextTerminationHandler = e -> currentManagedContext.terminate();
    }

    @Override
    @SuppressWarnings("unchecked") // ignore currentManagedContextTerminationHandler types, just use Object
    public void handle(final RoutingContext ctx) {

        if (currentManagedContext.isActive()) {
            handleWithIdentity(ctx);
        } else {

            currentManagedContext.activate();
            ctx.response()
                    .endHandler(currentManagedContextTerminationHandler)
                    .exceptionHandler(currentManagedContextTerminationHandler)
                    .closeHandler(currentManagedContextTerminationHandler);

            try {
                handleWithIdentity(ctx);
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

    private void handleWithIdentity(final RoutingContext ctx) {
        if (currentIdentityAssociation != null) {
            QuarkusHttpUser existing = (QuarkusHttpUser) ctx.user();
            if (existing != null) {
                SecurityIdentity identity = existing.getSecurityIdentity();
                currentIdentityAssociation.setIdentity(identity);
            } else {
                currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(ctx, null));
            }
        }
        currentVertxRequest.setCurrent(ctx);
        doHandle(ctx);
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
