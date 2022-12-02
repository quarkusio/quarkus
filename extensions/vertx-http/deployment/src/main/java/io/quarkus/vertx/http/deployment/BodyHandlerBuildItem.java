package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public final class BodyHandlerBuildItem extends SimpleBuildItem {
    private final Handler<RoutingContext> handler;

    public BodyHandlerBuildItem(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }
}
