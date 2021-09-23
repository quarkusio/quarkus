package io.quarkus.vertx.http.deployment.spi;

import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public interface NonApplicationRootPathBuilder {
    NonApplicationRootPathBuilder routeFunction(String route, Consumer<Route> routeFunction);

    NonApplicationRootPathBuilder route(String route);

    NonApplicationRootPathBuilder nestedRoute(String baseRoute, String subRoute);

    NonApplicationRootPathBuilder handler(Handler<RoutingContext> handler);

    NonApplicationRootPathBuilder blockingRoute();

    NonApplicationRootPathBuilder failureRoute();

    NonApplicationRootPathBuilder displayOnNotFoundPage();

    NonApplicationRootPathBuilder displayOnNotFoundPage(String notFoundPageTitle);

    NonApplicationRootPathBuilder routeConfigKey(String attributeName);
}
