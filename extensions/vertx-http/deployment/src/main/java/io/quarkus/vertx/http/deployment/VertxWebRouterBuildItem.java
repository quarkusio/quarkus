package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.ext.web.Router;

public final class VertxWebRouterBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Router> httpRouter;
    private final RuntimeValue<Router> mainRouter;
    private final RuntimeValue<Router> frameworkRouter;
    private final RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter;
    private final RuntimeValue<Router> managementRouter;

    VertxWebRouterBuildItem(RuntimeValue<Router> httpRouter, RuntimeValue<Router> mainRouter,
            RuntimeValue<Router> frameworkRouter,
            RuntimeValue<Router> managementRouter,
            RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter) {
        this.httpRouter = httpRouter;
        this.mainRouter = mainRouter;
        this.frameworkRouter = frameworkRouter;
        this.managementRouter = managementRouter; // Can be null if the management interface is disabled
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
    public RuntimeValue<Router> getMainRouter() {
        return mainRouter;
    }

    /**
     * Will be {@code null} if {@code ${quarkus.http.root-path}} is the same as
     * {@code ${quarkus.http.non-application-root-path}}.
     *
     * @return RuntimeValue<Router>
     */
    public RuntimeValue<Router> getFrameworkRouter() {
        return frameworkRouter;
    }

    /**
     * Will be {@code null} if the management interface is disabled.
     *
     * @return RuntimeValue<Router>
     */
    public RuntimeValue<Router> getManagementRouter() {
        return managementRouter;
    }
}
