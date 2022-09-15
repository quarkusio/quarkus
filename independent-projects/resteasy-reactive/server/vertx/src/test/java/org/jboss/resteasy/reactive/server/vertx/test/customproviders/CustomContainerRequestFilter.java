package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

public class CustomContainerRequestFilter {

    @ServerRequestFilter
    public void whatever(UriInfo uriInfo, HttpHeaders httpHeaders, ContainerRequestContext requestContext) {
        String customHeaderValue = uriInfo.getPath() + "-" + httpHeaders.getHeaderString("some-input");
        requestContext.getHeaders().putSingle("custom-header", customHeaderValue);
    }
}
