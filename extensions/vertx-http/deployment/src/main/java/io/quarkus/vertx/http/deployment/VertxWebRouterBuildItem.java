package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.ext.web.Router;

public final class VertxWebRouterBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Router> httpRouter;
    private final RuntimeValue<Router> mainRouter;
    private final RuntimeValue<Router> frameworkRouter;
    private final RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter;

    VertxWebRouterBuildItem(RuntimeValue<Router> httpRouter, RuntimeValue<Router> mainRouter,
            RuntimeValue<Router> frameworkRouter, RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter) {
        this.httpRouter = httpRouter;
        this.mainRouter = mainRouter;
        this.frameworkRouter = frameworkRouter;
        this.mutinyRouter = mutinyRouter;
    }

    public RuntimeValue<Router> getHttpRouter() {
        return httpRouter;
    }

    public RuntimeValue<io.vertx.mutiny.ext.web.Router> getMutinyRouter() {
        return mutinyRouter;
    }

    /**
     * Will be {@code null} if `${quarkus.http.root-path}` is {@literal /}.
     *
     * @return RuntimeValue<Router>
     */
    RuntimeValue<Router> getMainRouter() {
        return mainRouter;
    }

    /**
     * Will be {@code null} if {@code ${quarkus.http.root-path}} is the same as
     * {@code ${quarkus.http.non-application-root-path}}.
     *
     * @return RuntimeValue<Router>
     */
    RuntimeValue<Router> getFrameworkRouter() {
        return frameworkRouter;
    }
}
