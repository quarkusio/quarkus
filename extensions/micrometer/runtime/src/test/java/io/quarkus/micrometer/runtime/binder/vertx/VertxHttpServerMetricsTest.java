package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.export.exemplars.OpenTelemetryContextUnwrapper;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpServerMetricsTest {

    RoutingContext routingContext;
    HttpRequestMetric requestMetric;
    HttpServerRequestInternal request;

    @BeforeEach
    public void init() {
        requestMetric = new HttpRequestMetric("/irrelevant", new LongAdder());

        routingContext = Mockito.mock(RoutingContext.class);
        request = Mockito.mock(HttpServerRequestInternal.class);

        Mockito.when(routingContext.request()).thenReturn(request);
        Mockito.when(request.metric()).thenReturn(requestMetric);
    }

    @Test
    public void testReturnPathFromHttpRequestPath() {
        HttpRequestMetric fetchedMetric = HttpRequestMetric.getRequestMetric(routingContext);
        Assertions.assertSame(requestMetric, fetchedMetric);

        // Emulate a JAX-RS or Servlet filter pre-determining the template path
        requestMetric.setTemplatePath("/item/{id}");
        Assertions.assertEquals("/item/{id}", requestMetric.applyTemplateMatching("/"));
    }

    @Test
    public void testReturnRoutedPath() {
        // Vertx route information collection, no web template, will use normalized initial value
        requestMetric.appendCurrentRoutePath("/notused");
        // Return the value passed in as parameter (no templates)
        Assertions.assertEquals("/item/abc", requestMetric.applyTemplateMatching("/item/abc"));
    }

    @Test
    public void testQueryStringIsNotPartOfTheInitialPath() {
        // The initial path is compared with the measured path to tell a templated path apart from
        // a raw one, so it carries no query string either
        HttpRequestMetric metric = new HttpRequestMetric("/ws?room=lobby&user=alice", new LongAdder());

        Assertions.assertEquals("/ws", metric.getInitialPath());
    }

    @Test
    public void testReturnTemplatedPathFromRoutingContext() {
        // Emulate a Vert.x Route containing templated values
        requestMetric.appendCurrentRoutePath("/item/:id");

        // Should return the templated version of the path (based on the route definition)
        Assertions.assertEquals("/item/{id}", requestMetric.applyTemplateMatching("/"));
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", HttpRequestMetric.vertxWebToUriTemplate.get("/item/:id"));
    }

    @Test
    public void testRequestResetWithNonInternalHttpRequest() {
        HttpRequest httpReq = Mockito.mock(HttpRequest.class);
        Mockito.when(httpReq.method()).thenReturn(HttpMethod.GET);
        Mockito.when(httpReq.uri()).thenReturn("/test");

        LongAdder activeRequests = new LongAdder();
        HttpRequestMetric metric = new HttpRequestMetric(httpReq, activeRequests);
        Assertions.assertNull(metric.request(), "request() must be null for non-internal HttpRequest");
        Assertions.assertNotNull(metric.httpRequest());

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        HttpBinderConfiguration config = Mockito.mock(HttpBinderConfiguration.class);
        Mockito.when(config.getHttpServerActiveRequestsName()).thenReturn("http.server.active.requests");
        Mockito.when(config.getHttpServerRequestsName()).thenReturn("http.server.requests");
        Mockito.when(config.getHttpServerWebSocketConnectionsName()).thenReturn("http.server.websocket.connections");
        Mockito.when(config.getHttpServerPushName()).thenReturn("http.server.push");
        Mockito.when(config.getServerMatchPatterns()).thenReturn(Collections.emptyMap());
        Mockito.when(config.getServerIgnorePatterns()).thenReturn(Collections.emptyList());

        OpenTelemetryContextUnwrapper unwrapper = new OpenTelemetryContextUnwrapper() {
            @Override
            public <P, R> R executeInContext(Function<P, R> fn, P param, Context ctx) {
                return fn.apply(param);
            }
        };
        VertxHttpServerMetrics serverMetrics = new VertxHttpServerMetrics(
                registry, config, unwrapper,
                new HttpServerConfig(), Gauges.longAdder());

        metric.setSample(Timer.start(registry));
        metric.requestStarted();

        serverMetrics.requestReset(metric);

        Assertions.assertEquals(0, activeRequests.sum(), "activeRequests must be decremented after reset");
        Timer timer = registry.find("http.server.requests").tag("method", "GET").timer();
        Assertions.assertNotNull(timer, "timer must be recorded with method tag from httpRequest()");
        Assertions.assertEquals(1, timer.count());
    }

}
