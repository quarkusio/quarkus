package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.ext.web.Router;

public final class VertxWebRouterBuildItem extends SimpleBuildItem {

    private RuntimeValue<Router> httpRouter;
    private RuntimeValue<Router> mainRouter;
    private RuntimeValue<Router> frameworkRouter;

    VertxWebRouterBuildItem(RuntimeValue<Router> httpRouter, RuntimeValue<Router> mainRouter,
            RuntimeValue<Router> frameworkRouter) {
        this.httpRouter = httpRouter;
        this.mainRouter = mainRouter;
        this.frameworkRouter = frameworkRouter;
    }

    public RuntimeValue<Router> getHttpRouter() {
        return httpRouter;
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
