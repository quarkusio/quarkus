package io.quarkus.vertx.core.deployment;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.vertx.core.Vertx;

public final class CoreVertxBuildItem extends SimpleBuildItem {

    private final Supplier<Vertx> vertx;

    public CoreVertxBuildItem(Supplier<Vertx> vertx) {
        this.vertx = vertx;
    }

    public Supplier<Vertx> getVertx() {
        return vertx;
    }

}
