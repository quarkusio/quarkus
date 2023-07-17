package io.quarkus.vertx.web.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * use {@link io.quarkus.vertx.http.deployment.BodyHandlerBuildItem} instead
 */
@Deprecated
public final class BodyHandlerBuildItem extends SimpleBuildItem {
    private final Handler<RoutingContext> handler;

    public BodyHandlerBuildItem(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }
}
