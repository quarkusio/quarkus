package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Disabled on Java 8 because of Mocks
 */
@DisabledOnJre(JRE.JAVA_8)
public class VertxHttpServerMetricsTest {
    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();

    Route currentRoute;
    RoutingContext routingContext;
    HttpRequestMetric requestMetric;
    HttpServerRequest request;

    @BeforeEach
    public void init() {
        requestMetric = new HttpRequestMetric("/irrelevant");

        currentRoute = Mockito.mock(Route.class);
        routingContext = Mockito.mock(RoutingContext.class);
        request = Mockito.mock(HttpServerRequest.class);

        Mockito.when(routingContext.request()).thenReturn(request);
        Mockito.when(routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(routingContext.get(VertxHttpServerMetrics.METRICS_CONTEXT)).thenReturn(routingContext);
    }

    @Test
    public void testReturnPathFromHttpRequestPath() {
        // Emulate a JAX-RS or Servlet filter pre-determining the template path
        requestMetric.setTemplatePath("/item/{id}");
        Assertions.assertEquals("/item/{id}", requestMetric.applyTemplateMatching("/"));
    }

    @Test
    public void testReturnPathFromRoutingContext() {
        // Vertx route information collection, no web template
        Mockito.when(currentRoute.getPath()).thenReturn("/item");
        Mockito.when(routingContext.mountPoint()).thenReturn("/");
        requestMetric.routingContext = routingContext;

        // Return the value passed in from the parent class
        Assertions.assertEquals("/item/abc", requestMetric.applyTemplateMatching("/item/abc"));
    }

    @Test
    public void testReturnGenericPathFromRoutingContext() {
        // Emulate a Vert.x Route containing templated values
        Mockito.when(currentRoute.getPath()).thenReturn("/item/:id");
        Mockito.when(routingContext.mountPoint()).thenReturn("/");
        requestMetric.routingContext = routingContext;

        // Should return the templated version of the path (based on the route definition)
        Assertions.assertEquals("/item/{id}", requestMetric.applyTemplateMatching("/"));
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", HttpRequestMetric.vertxWebToUriTemplate.get("/item/:id"));
    }
}
