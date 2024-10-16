package io.quarkus.vertx.http.deployment;

import io.quarkus.vertx.http.runtime.HandlerType;

/**
 * Convert the route build item from the SPI to the internal representation
 */
public class RouteConverter {

    public static RouteBuildItem convert(io.quarkus.vertx.http.deployment.spi.RouteBuildItem item,
            HttpRootPathBuildItem httpRootPathBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        // The builder depends on the type of route
        RouteBuildItem.Builder builder;
        if (item.getTypeOfRoute() == io.quarkus.vertx.http.deployment.spi.RouteBuildItem.RouteType.FRAMEWORK_ROUTE) {
            builder = nonApplicationRootPathBuildItem.routeBuilder();
        } else {
            builder = httpRootPathBuildItem.routeBuilder();
        }

        if (item.isManagement()) {
            builder = builder.management();
        }
        if (item.hasRouteConfigKey()) {
            builder = builder.routeConfigKey(item.getRouteConfigKey());
        }

        builder = builder.handler(item.getHandler()).handlerType(HandlerType.valueOf(item.getHandlerType().name()));
        if (item.isDisplayOnNotFoundPage()) {
            builder = builder
                    .displayOnNotFoundPage(item.getNotFoundPageTitle());
        }

        if (item.hasOrder()) {
            builder = builder.orderedRoute(item.getPath(), item.getOrder(), item.getCustomizer());
        } else {
            builder = builder.routeFunction(item.getPath(), item.getCustomizer());
        }

        return builder.build();

    }

}
