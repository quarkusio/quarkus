package org.jboss.resteasy.reactive.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.Executor;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ResteasyReactiveRequestContextTest {

    @Test
    void testAbsoluteUriWithOverrides() {
        var request = Mockito.mock(ServerHttpRequest.class);
        var context = new ResteasyReactiveRequestContext(null, null, null, null) {

            @Override
            public ServerHttpResponse serverResponse() {
                return null;
            }

            @Override
            public ServerHttpRequest serverRequest() {
                return request;
            }

            @Override
            public boolean resumeExternalProcessing() {
                return false;
            }

            @Override
            public Runnable registerTimer(long millis, Runnable task) {
                return null;
            }

            @Override
            protected Executor getEventLoop() {
                return null;
            }

            @Override
            protected void setQueryParamsFrom(String uri) {

            }
        };
        Mockito.when(request.getRequestNormalisedPath()).thenReturn("/path;a");
        Mockito.when(request.getRequestScheme()).thenReturn("http");
        Mockito.when(request.getRequestHost()).thenReturn("host:port");

        context.initPathSegments();
        assertEquals("http://host:port/path", context.getAbsoluteURI());

        context.setRequestUri(URI.create("https://host1:port1/path1"));
        assertEquals("https://host1:port1/path1", context.getAbsoluteURI());
    }

}
