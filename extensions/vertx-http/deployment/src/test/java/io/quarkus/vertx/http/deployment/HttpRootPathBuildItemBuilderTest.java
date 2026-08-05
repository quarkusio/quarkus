package io.quarkus.vertx.http.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class HttpRootPathBuildItemBuilderTest {

    private final Handler<RoutingContext> handler = rc -> {
        // Empty on purpose.
    };

    @Test
    void orderedRoute_relativePathWithDefaultRoot_usesApplicationRoute() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .orderedRoute("foo", 10)
                .handler(handler)
                .build();

        assertEquals(RouteBuildItem.RouteType.APPLICATION_ROUTE, route.getRouterType());
        assertEquals("/foo", route.getAbsolutePath());
    }

    @Test
    void orderedRoute_absolutePathOutsideRoot_usesAbsoluteRoute() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        RouteBuildItem route = buildItem.routeBuilder()
                .orderedRoute("/outside", 10)
                .handler(handler)
                .build();

        assertEquals(RouteBuildItem.RouteType.ABSOLUTE_ROUTE, route.getRouterType());
        assertEquals("/outside", route.getAbsolutePath());
    }

    @Test
    void orderedRoute_absolutePathInsideRoot_usesApplicationRoute() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        RouteBuildItem route = buildItem.routeBuilder()
                .orderedRoute("endpoint", 5)
                .handler(handler)
                .build();

        assertEquals(RouteBuildItem.RouteType.APPLICATION_ROUTE, route.getRouterType());
        assertEquals("/app/endpoint", route.getAbsolutePath());
    }

    @Test
    void routeFunction_throwsForRawFunction() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        HttpRootPathBuildItem.Builder builder = buildItem.routeBuilder();

        assertThrows(RuntimeException.class, () -> builder.routeFunction(router -> null));
    }

    @Test
    void routeFunction_withPathAndConsumer_setsRouteFunction() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .routeFunction("test", routeObj -> {
                })
                .handler(handler)
                .build();

        assertNotNull(route.getRouteFunction());
        assertEquals("/test", route.getAbsolutePath());
    }

    @Test
    void nestedRoute_absoluteSubRoute_treatedAsAbsolute() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        RouteBuildItem route = buildItem.routeBuilder()
                .nestedRoute("/base", "/absolute")
                .handler(handler)
                .build();

        // Absolute sub-route is resolved independently
        assertEquals("/absolute", route.getAbsolutePath());
    }

    @Test
    void nestedRoute_relativeSubRoute_appendedToBase() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .nestedRoute("/base", "sub")
                .handler(handler)
                .build();

        assertEquals("/base/sub", route.getAbsolutePath());
    }

    @Test
    void nestedRoute_baseWithoutTrailingSlash_slashAdded() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .nestedRoute("/base", "sub")
                .handler(handler)
                .build();

        assertEquals("/base/sub", route.getAbsolutePath());
    }

    @Test
    void route_setsDisplayOnNotFoundPage() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .route("test")
                .handler(handler)
                .displayOnNotFoundPage("Test Title")
                .build();

        assertNotNull(route.getNotFoundPageDisplayableEndpoint());
        assertTrue(route.getNotFoundPageDisplayableEndpoint().isAbsolutePath());
    }

    @Test
    void route_setsRouteConfigKey() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .route("test")
                .handler(handler)
                .routeConfigKey("my.key")
                .build();

        assertNotNull(route.getConfiguredPathInfo());
        assertEquals("my.key", route.getConfiguredPathInfo().getName());
    }

    @Test
    void management_setsManagementFlag() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        RouteBuildItem route = buildItem.routeBuilder()
                .route("test")
                .handler(handler)
                .management()
                .build();

        assertTrue(route.isManagement());
    }
}
