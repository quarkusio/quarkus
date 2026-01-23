package org.jboss.resteasy.reactive.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ForwardedInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

public class LocationUtilTest {

    static ResteasyReactiveRequestContext mockContext(String authority) {
        var context = Mockito.mock(ResteasyReactiveRequestContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.getScheme()).thenReturn("https");
        Mockito.when(context.getAuthority()).thenReturn(authority);
        Mockito.when(context.getDeployment().getPrefix()).thenReturn("/prefix");
        return context;
    }

    @Test
    public void uriWithEmtpyAuthority() {
        assertEquals("https:///", LocationUtil.getUri("", mockContext(""), false).toString());
    }

    @Test
    public void uriWithRelativeUri() throws URISyntaxException {
        assertEquals("https://host:999/prefix/a?b",
                LocationUtil.getUri("a?b", mockContext("host:999"), true).toString());
    }

    @Test
    public void uriWithNullAuthority() {
        assertEquals("https:/", LocationUtil.getUri("", mockContext(null), false).toString());
    }

    @ParameterizedTest
    @CsvSource({
            "''                 , https://host:999/prefix/",
            "forwarded-prefix   , https://host:999/forwarded-prefix/prefix/",
            "/forwarded-prefix  , https://host:999/forwarded-prefix/prefix/",
            "forwarded-prefix/  , https://host:999/forwarded-prefix/prefix/",
            "/forwarded-prefix/ , https://host:999/forwarded-prefix/prefix/",
    })
    public void uriWithForwardedPrefix(String forwardedPrefix, String expected) {
        var context = mockContext("host:999");
        var serverRequest = Mockito.mock(ServerHttpRequest.class);
        var forwardedInfo = Mockito.mock(ForwardedInfo.class);
        Mockito.when(context.serverRequest()).thenReturn(serverRequest);
        Mockito.when(serverRequest.getForwardedInfo()).thenReturn(forwardedInfo);
        Mockito.when(forwardedInfo.getPrefix()).thenReturn(forwardedPrefix);
        assertEquals(expected, LocationUtil.getUri("", context, true).toString());
    }
}
