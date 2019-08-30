package io.quarkus.vertx.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Vertx;

/**
 * TODO: DELETE THIS CLASS
 * there should only be one vert.x instance, once RESTeasy has support for async IO this should be deleted
 */
@Deprecated
public final class WebVertxBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Vertx> vertx;

    public WebVertxBuildItem(RuntimeValue<Vertx> vertx) {
        this.vertx = vertx;
    }

    public RuntimeValue<Vertx> getVertx() {
        return vertx;
    }

}
