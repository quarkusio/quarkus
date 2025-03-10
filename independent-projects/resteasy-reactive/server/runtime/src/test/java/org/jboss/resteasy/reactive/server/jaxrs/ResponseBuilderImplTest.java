package org.jboss.resteasy.reactive.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ResponseBuilderImplTest {

    @Test
    public void shouldBuildWithNonAbsoulteLocationAndIPv6Address() {
        var context = Mockito.mock(ResteasyReactiveRequestContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.serverRequest().getRequestHost()).thenReturn("[0:0:0:0:0:0:0:1]");
        Mockito.when(context.getDeployment().getPrefix()).thenReturn("/prefix");
        CurrentRequestManager.set(context);
        var response = ResponseBuilderImpl.ok().location(URI.create("/host")).build();
        assertEquals("//[0:0:0:0:0:0:0:1]/prefix/host", response.getLocation().toString());

        response = ResponseBuilderImpl.ok().contentLocation(URI.create("/host")).build();
        assertEquals("//[0:0:0:0:0:0:0:1]/host", response.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION).toString());
    }

}
