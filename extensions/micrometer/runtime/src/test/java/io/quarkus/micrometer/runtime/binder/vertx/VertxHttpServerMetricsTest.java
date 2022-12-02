package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.ext.web.RoutingContext;

/**
 * Disabled on Java 8 because of Mocks
 */
@DisabledOnJre(JRE.JAVA_8)
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
    public void testReturnTemplatedPathFromRoutingContext() {
        // Emulate a Vert.x Route containing templated values
        requestMetric.appendCurrentRoutePath("/item/:id");

        // Should return the templated version of the path (based on the route definition)
        Assertions.assertEquals("/item/{id}", requestMetric.applyTemplateMatching("/"));
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", HttpRequestMetric.vertxWebToUriTemplate.get("/item/:id"));
    }

}
