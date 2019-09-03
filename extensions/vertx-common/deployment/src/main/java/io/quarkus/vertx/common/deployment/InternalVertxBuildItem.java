package io.quarkus.vertx.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Vertx;

public final class InternalVertxBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Vertx> vertx;

    public InternalVertxBuildItem(RuntimeValue<Vertx> vertx) {
        this.vertx = vertx;
    }

    public RuntimeValue<Vertx> getVertx() {
        return vertx;
    }

}
