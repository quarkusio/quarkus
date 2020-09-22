package io.quarkus.micrometer.runtime.binder.vertx;

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
public class RequestMetricTest {

    @Test
    public void testReturnPathFromHttpRequestPath() {
        RequestMetric requestMetric = new RequestMetric();
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);

        Mockito.when(requestMetric.routingContext.get(RequestMetric.HTTP_REQUEST_PATH))
                .thenReturn("/item/{id}");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnPathFromRoutingContext() {
        RequestMetric requestMetric = new RequestMetric();
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item");

        Assertions.assertEquals("/item", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnGenericPathFromRoutingContext() {
        RequestMetric requestMetric = new RequestMetric();
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item/:id");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", RequestMetric.vertxRoutePath.get("/item/:id"));
    }
}
