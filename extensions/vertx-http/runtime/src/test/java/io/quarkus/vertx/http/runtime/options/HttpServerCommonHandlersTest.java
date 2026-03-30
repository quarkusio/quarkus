package io.quarkus.vertx.http.runtime.options;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.vertx.http.runtime.FilterConfig;
import io.quarkus.vertx.http.runtime.HeaderConfig;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpServerCommonHandlersTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    Router router;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    RoutingContext routingContext;

    @Mock
    ServerLimitsConfig limitsConfig;

    @Test
    void enforceMaxBodySize_notPresent_noRouteRegistered() {
        when(limitsConfig.maxBodySize()).thenReturn(Optional.empty());

        HttpServerCommonHandlers.enforceMaxBodySize(limitsConfig, router);

        verify(router, never()).route();
    }

    @Test
    @SuppressWarnings("unchecked")
    void enforceMaxBodySize_present_routeRegisteredWithCorrectOrder() {
        MemorySize memorySize = mock(MemorySize.class);
        when(memorySize.asLongValue()).thenReturn(1024L);
        when(limitsConfig.maxBodySize()).thenReturn(Optional.of(memorySize));

        Route route = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route orderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(router.route()).thenReturn(route);
        when(route.order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT)).thenReturn(orderedRoute);

        HttpServerCommonHandlers.enforceMaxBodySize(limitsConfig, router);

        verify(route).order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT);
        verify(orderedRoute).handler(any(Handler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void enforceMaxBodySize_contentLengthOverLimit_returns413() {
        Handler<RoutingContext> handler = captureMaxBodySizeHandler(1024L);

        HttpServerRequest request = mock(HttpServerRequest.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        HttpServerResponse response = mock(HttpServerResponse.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        MultiMap requestHeaders = mock(MultiMap.class);
        MultiMap responseHeaders = mock(MultiMap.class);

        when(routingContext.request()).thenReturn(request);
        when(request.headers()).thenReturn(requestHeaders);
        when(requestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn("2048");
        when(routingContext.response()).thenReturn(response);
        when(response.headers()).thenReturn(responseHeaders);
        when(responseHeaders.add(any(CharSequence.class), any(CharSequence.class))).thenReturn(responseHeaders);
        when(response.setStatusCode(413)).thenReturn(response);

        handler.handle(routingContext);

        verify(responseHeaders).add(HttpHeaderNames.CONNECTION, "close");
        verify(response).setStatusCode(413);
        verify(response).endHandler(any(Handler.class));
        verify(response).end();
        verify(routingContext, never()).next();
    }

    @Test
    void enforceMaxBodySize_contentLengthUnderLimit_callsNext() {
        Handler<RoutingContext> handler = captureMaxBodySizeHandler(1024L);

        HttpServerRequest request = mock(HttpServerRequest.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        MultiMap requestHeaders = mock(MultiMap.class);

        when(routingContext.request()).thenReturn(request);
        when(request.headers()).thenReturn(requestHeaders);
        when(requestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn("512");

        handler.handle(routingContext);

        verify(routingContext).next();
    }

    @Test
    @SuppressWarnings("unchecked")
    void enforceMaxBodySize_noContentLengthHeader_setsMaxRequestSizeKey() {
        long limit = 1024L;
        Handler<RoutingContext> handler = captureMaxBodySizeHandler(limit);

        HttpServerRequest request = mock(HttpServerRequest.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        MultiMap requestHeaders = mock(MultiMap.class);

        when(routingContext.request()).thenReturn(request);
        when(request.headers()).thenReturn(requestHeaders);
        when(requestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn(null);

        handler.handle(routingContext);

        verify(routingContext).put(eq(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY), eq(Long.valueOf(limit)));
        verify(routingContext).next();
    }

    @Test
    void applyHeaders_emptyMap_noRoutesAdded() {
        HttpServerCommonHandlers.applyHeaders(Collections.emptyMap(), router);

        verify(router, never()).route(anyString());
        verify(router, never()).route(any(HttpMethod.class), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyHeaders_noMethods_routeWithPathOnly() {
        HeaderConfig headerConfig = mock(HeaderConfig.class);
        when(headerConfig.path()).thenReturn("/api/*");
        when(headerConfig.value()).thenReturn("bar");
        when(headerConfig.methods()).thenReturn(Optional.empty());

        Route route = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route orderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(router.route("/api/*")).thenReturn(route);
        when(route.order(RouteConstants.ROUTE_ORDER_HEADERS)).thenReturn(orderedRoute);

        Map<String, HeaderConfig> headers = new LinkedHashMap<>();
        headers.put("X-Custom", headerConfig);

        HttpServerCommonHandlers.applyHeaders(headers, router);

        verify(router).route("/api/*");
        verify(route).order(RouteConstants.ROUTE_ORDER_HEADERS);
        verify(orderedRoute).handler(any(Handler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyHeaders_withMethods_routePerMethod() {
        HeaderConfig headerConfig = mock(HeaderConfig.class);
        when(headerConfig.path()).thenReturn("/api/*");
        when(headerConfig.value()).thenReturn("bar");
        when(headerConfig.methods()).thenReturn(Optional.of(List.of("GET", "POST")));

        Route getRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route getOrderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route postRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route postOrderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        when(router.route(HttpMethod.GET, "/api/*")).thenReturn(getRoute);
        when(getRoute.order(RouteConstants.ROUTE_ORDER_HEADERS)).thenReturn(getOrderedRoute);
        when(router.route(HttpMethod.POST, "/api/*")).thenReturn(postRoute);
        when(postRoute.order(RouteConstants.ROUTE_ORDER_HEADERS)).thenReturn(postOrderedRoute);

        Map<String, HeaderConfig> headers = new LinkedHashMap<>();
        headers.put("X-Custom", headerConfig);

        HttpServerCommonHandlers.applyHeaders(headers, router);

        verify(router).route(HttpMethod.GET, "/api/*");
        verify(router).route(HttpMethod.POST, "/api/*");
        verify(getOrderedRoute).handler(any(Handler.class));
        verify(postOrderedRoute).handler(any(Handler.class));
    }

    @Test
    void applyFilters_emptyMap_noRoutesAdded() {
        HttpServerCommonHandlers.applyFilters(Collections.emptyMap(), router);

        verify(router, never()).routeWithRegex(anyString());
        verify(router, never()).routeWithRegex(any(HttpMethod.class), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyFilters_noMethods_routeWithRegexForAllMethods() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.matches()).thenReturn("/api/.*");
        when(filterConfig.order()).thenReturn(OptionalInt.of(10));
        when(filterConfig.methods()).thenReturn(Optional.empty());
        when(filterConfig.header()).thenReturn(Map.of("X-Filter", "value"));

        Route route = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route orderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(router.routeWithRegex("/api/.*")).thenReturn(route);
        when(route.order(10)).thenReturn(orderedRoute);

        Map<String, FilterConfig> filters = new LinkedHashMap<>();
        filters.put("filter1", filterConfig);

        HttpServerCommonHandlers.applyFilters(filters, router);

        verify(router).routeWithRegex("/api/.*");
        verify(route).order(10);
        verify(orderedRoute).handler(any(Handler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyFilters_withMethods_routeWithRegexPerMethod() {
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.matches()).thenReturn("/api/.*");
        when(filterConfig.order()).thenReturn(OptionalInt.of(5));
        when(filterConfig.methods()).thenReturn(Optional.of(List.of("GET", "POST")));
        when(filterConfig.header()).thenReturn(Map.of("X-Filter", "value"));

        Route getRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route getOrderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route postRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route postOrderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        when(router.routeWithRegex(HttpMethod.GET, "/api/.*")).thenReturn(getRoute);
        when(getRoute.order(5)).thenReturn(getOrderedRoute);
        when(router.routeWithRegex(HttpMethod.POST, "/api/.*")).thenReturn(postRoute);
        when(postRoute.order(5)).thenReturn(postOrderedRoute);

        Map<String, FilterConfig> filters = new LinkedHashMap<>();
        filters.put("filter1", filterConfig);

        HttpServerCommonHandlers.applyFilters(filters, router);

        verify(router).routeWithRegex(HttpMethod.GET, "/api/.*");
        verify(router).routeWithRegex(HttpMethod.POST, "/api/.*");
        verify(getOrderedRoute).handler(any(Handler.class));
        verify(postOrderedRoute).handler(any(Handler.class));
    }

    @SuppressWarnings("unchecked")
    private Handler<RoutingContext> captureMaxBodySizeHandler(long limit) {
        MemorySize memorySize = mock(MemorySize.class);
        when(memorySize.asLongValue()).thenReturn(limit);
        when(limitsConfig.maxBodySize()).thenReturn(Optional.of(memorySize));

        Route route = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Route orderedRoute = mock(Route.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(router.route()).thenReturn(route);
        when(route.order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT)).thenReturn(orderedRoute);

        ArgumentCaptor<Handler<RoutingContext>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        HttpServerCommonHandlers.enforceMaxBodySize(limitsConfig, router);
        verify(orderedRoute).handler(handlerCaptor.capture());
        return handlerCaptor.getValue();
    }
}
