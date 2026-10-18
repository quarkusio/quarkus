package io.quarkus.vertx.http.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class RouteBuildItemTest {

    private final Handler<RoutingContext> handler = rc -> {
        // Empty on purpose.
    };

    @Test
    void build_withoutRouteFunction_throws() {
        RouteBuildItem.Builder builder = RouteBuildItem.builder();
        builder.handler(handler);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_withRoute_succeeds() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .build();

        assertSame(handler, item.getHandler());
        assertNotNull(item.getRouteFunction());
        assertEquals(HandlerType.NORMAL, item.getType());
    }

    @Test
    void build_blockingRoute_setsHandlerType() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .blockingRoute()
                .build();

        assertEquals(HandlerType.BLOCKING, item.getType());
    }

    @Test
    void build_failureRoute_setsHandlerType() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .failureRoute()
                .build();

        assertEquals(HandlerType.FAILURE, item.getType());
    }

    @Test
    void build_handlerType_overrides() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .handlerType(HandlerType.BLOCKING)
                .build();

        assertEquals(HandlerType.BLOCKING, item.getType());
    }

    @Test
    void build_defaultRouteType_isApplicationRoute() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .build();

        assertEquals(RouteBuildItem.RouteType.APPLICATION_ROUTE, item.getRouteType());
        assertEquals(RouteBuildItem.RouteType.APPLICATION_ROUTE, item.getRouterType());
        assertTrue(item.isRouterApplication());
        assertFalse(item.isRouterFramework());
        assertFalse(item.isRouterAbsolute());
    }

    @Test
    void build_notManagementByDefault() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .build();

        assertFalse(item.isManagement());
    }

    @Test
    void build_managementWithoutConfigKey() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .management()
                .build();

        assertTrue(item.isManagement());
    }

    @Test
    void getNotFoundEndpoint_notDisplayed_returnsNull() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .build();

        assertNull(item.getNotFoundPageDisplayableEndpoint());
    }

    @Test
    void getNotFoundEndpoint_displayedWithTitle() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .displayOnNotFoundPage("Test Page")
                .build();

        assertNotNull(item.getNotFoundPageDisplayableEndpoint());
        assertEquals("Test Page", item.getNotFoundPageDisplayableEndpoint().getDescription());
        assertEquals("/test", item.getNotFoundPageDisplayableEndpoint().getEndpoint());
    }

    @Test
    void getNotFoundEndpoint_displayedWithoutTitle() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .displayOnNotFoundPage()
                .build();

        assertNotNull(item.getNotFoundPageDisplayableEndpoint());
        assertNull(item.getNotFoundPageDisplayableEndpoint().getDescription());
    }

    @Test
    void getConfiguredPathInfo_noConfigKey_returnsNull() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .build();

        assertNull(item.getConfiguredPathInfo());
    }

    @Test
    void getConfiguredPathInfo_withConfigKey_returnsInfo() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/test")
                .handler(handler)
                .routeConfigKey("my.config.key")
                .build();

        assertNotNull(item.getConfiguredPathInfo());
        assertEquals("my.config.key", item.getConfiguredPathInfo().getName());
    }

    @Test
    void orderedRoute_setsRouteFunction() {
        RouteBuildItem item = RouteBuildItem.builder()
                .orderedRoute("/test", 100)
                .handler(handler)
                .build();

        assertNotNull(item.getRouteFunction());
        assertSame(handler, item.getHandler());
    }

    @Test
    void orderedRoute_withCustomizer_setsRouteFunction() {
        RouteBuildItem item = RouteBuildItem.builder()
                .orderedRoute("/test", 100, route -> {
                })
                .handler(handler)
                .build();

        assertNotNull(item.getRouteFunction());
    }

    @Test
    void getAbsolutePath_fromRoute() {
        RouteBuildItem item = RouteBuildItem.builder()
                .route("/my/path")
                .handler(handler)
                .build();

        // For the base builder, absolutePath is set via routePath
        assertNull(item.getAbsolutePath());
    }
}
