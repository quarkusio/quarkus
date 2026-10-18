package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.ext.web.Router;

final class InitialRouterBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Router> httpRouter;
    private final RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter;

    public InitialRouterBuildItem(RuntimeValue<Router> httpRouter, RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter) {
        this.httpRouter = httpRouter;
        this.mutinyRouter = mutinyRouter;
    }

    public RuntimeValue<Router> getHttpRouter() {
        return httpRouter;
    }

    public RuntimeValue<io.vertx.mutiny.ext.web.Router> getMutinyRouter() {
        return mutinyRouter;
    }

}
