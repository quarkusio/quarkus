package org.jboss.resteasy.reactive.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.junit.jupiter.api.Test;

public class RestResponseBuilderImplTest {

    @Test
    public void shouldBuildWithNonAbsoulteLocationAndIPv6Address() {
        var context = LocationUtilTest.mockContext("[0:0:0:0:0:0:0:1]");
        CurrentRequestManager.set(context);
        var response = RestResponseBuilderImpl.ok().location(URI.create("/host")).build();
        assertEquals("https://[0:0:0:0:0:0:0:1]/prefix/host", response.getLocation().toString());

        response = RestResponseBuilderImpl.ok().contentLocation(URI.create("/host")).build();
        assertEquals("https://[0:0:0:0:0:0:0:1]/host", response.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION).toString());
    }

}
