package io.quarkus.vertx.http.runtime.webjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

class WebJarStaticHandlerTest {

    @Test
    void redirectWithoutTrailingSlashUsesRelativeLocation() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        when(context.normalizedPath()).thenReturn("/q/swagger-ui");
        when(context.response()).thenReturn(response);
        when(response.headers()).thenReturn(headers);

        new WebJarStaticHandler(null, "/q/swagger-ui", null).handle(context);

        assertThat(headers.get(HttpHeaders.LOCATION)).isEqualTo("swagger-ui/");
        verify(response).setStatusCode(302);
        verify(response).end();
    }

    @Test
    void redirectRootLevelPathUsesRelativeLocation() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        when(context.normalizedPath()).thenReturn("/swagger-ui");
        when(context.response()).thenReturn(response);
        when(response.headers()).thenReturn(headers);

        new WebJarStaticHandler(null, "/swagger-ui", null).handle(context);

        assertThat(headers.get(HttpHeaders.LOCATION)).isEqualTo("swagger-ui/");
        verify(response).setStatusCode(302);
        verify(response).end();
    }
}
