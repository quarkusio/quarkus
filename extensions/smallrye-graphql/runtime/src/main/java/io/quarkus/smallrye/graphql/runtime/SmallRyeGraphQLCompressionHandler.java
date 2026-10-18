package io.quarkus.smallrye.graphql.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeGraphQLCompressionHandler implements Handler<RoutingContext> {

    private final Handler<RoutingContext> routeHandler;

    public SmallRyeGraphQLCompressionHandler(Handler<RoutingContext> routeHandler) {
        this.routeHandler = routeHandler;
    }

    @Override
    public void handle(RoutingContext context) {
        context.addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                context.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
            }
        });
        routeHandler.handle(context);
    }
}
