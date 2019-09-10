package io.quarkus.vertx.core.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Vertx;

public final class CoreVertxBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Vertx> vertx;

    public CoreVertxBuildItem(RuntimeValue<Vertx> vertx) {
        this.vertx = vertx;
    }

    public RuntimeValue<Vertx> getVertx() {
        return vertx;
    }

}
