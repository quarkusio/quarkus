package io.quarkus.vertx.http.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class FilterBuildItemTest {

    private final Handler<RoutingContext> handler = rc -> {
    };

    @Test
    void constructor_with_valid_priority() {
        FilterBuildItem item = new FilterBuildItem(handler, 10);
        assertEquals(10, item.getPriority());
    }

    @Test
    void constructor_with_negative_priority_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FilterBuildItem(handler, -1));
    }

    @Test
    void constructor_with_zero_priority_succeeds() {
        FilterBuildItem item = new FilterBuildItem(handler, 0);
        assertEquals(0, item.getPriority());
    }

    @Test
    void toFilter_returns_correct_handler_and_priority() {
        FilterBuildItem item = new FilterBuildItem(handler, 42);
        Filter filter = item.toFilter();

        assertSame(handler, filter.getHandler());
        assertEquals(42, filter.getPriority());
        assertFalse(filter.isFailureHandler());
    }

    @Test
    void ofAuthenticationFailureHandler_creates_correct_item() {
        FilterBuildItem item = FilterBuildItem.ofAuthenticationFailureHandler(handler);

        assertSame(handler, item.getHandler());
        assertEquals(SecurityHandlerPriorities.AUTH_FAILURE_HANDLER, item.getPriority());
        assertTrue(item.isFailureHandler());
    }

    @Test
    void ofPreAuthenticationFailureHandler_creates_correct_item() {
        FilterBuildItem item = FilterBuildItem.ofPreAuthenticationFailureHandler(handler);

        assertSame(handler, item.getHandler());
        assertEquals(SecurityHandlerPriorities.AUTH_FAILURE_HANDLER + 1, item.getPriority());
        assertTrue(item.isFailureHandler());
    }

    @Test
    void getHandler_returns_handler() {
        FilterBuildItem item = new FilterBuildItem(handler, 5);
        assertSame(handler, item.getHandler());
    }

    @Test
    void getPriority_returns_priority() {
        FilterBuildItem item = new FilterBuildItem(handler, 99);
        assertEquals(99, item.getPriority());
    }

    @Test
    void isFailureHandler_false_by_default() {
        FilterBuildItem item = new FilterBuildItem(handler, 1);
        assertFalse(item.isFailureHandler());
    }

    @Test
    void isFailureHandler_true_for_auth_failure() {
        FilterBuildItem item = FilterBuildItem.ofAuthenticationFailureHandler(handler);
        assertTrue(item.isFailureHandler());
    }
}
