package org.jboss.resteasy.reactive.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.junit.jupiter.api.Test;
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

}
