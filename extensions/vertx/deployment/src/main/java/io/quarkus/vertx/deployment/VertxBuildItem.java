package io.quarkus.vertx.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Vertx;

public final class VertxBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Vertx> vertx;

    public VertxBuildItem(RuntimeValue<Vertx> vertx) {
        this.vertx = vertx;
    }

    public RuntimeValue<Vertx> getVertx() {
        return vertx;
    }

}
