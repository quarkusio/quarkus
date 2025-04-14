package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class CustomContainerRequestFilter {

    @ServerRequestFilter
    public void whatever(UriInfo uriInfo, HttpHeaders httpHeaders, ContainerRequestContext requestContext,
            ServerRequestContext serverRequestContext) {
        String customHeaderValue = uriInfo.getPath() + "-" + httpHeaders.getHeaderString("some-input") + "-"
                + serverRequestContext.getRequestHeaders().getHeaderString("some-other-input");
        requestContext.getHeaders().putSingle("custom-header", customHeaderValue);
    }
}
