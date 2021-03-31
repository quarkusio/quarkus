package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Disabled on Java 8 because of Mocks
 */
@DisabledOnJre(JRE.JAVA_8)
public class VertxHttpServerMetricsTest {
    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();

    @Test
    public void testReturnPathFromHttpRequestPath() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);

        Mockito.when(requestMetric.routingContext.get(HttpRequestMetric.HTTP_REQUEST_PATH))
                .thenReturn("/item/{id}");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnPathFromRoutingContext() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item");

        Assertions.assertEquals("/item", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnGenericPathFromRoutingContext() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item/:id");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", HttpRequestMetric.templatePath.get("/item/:id"));
    }

}
