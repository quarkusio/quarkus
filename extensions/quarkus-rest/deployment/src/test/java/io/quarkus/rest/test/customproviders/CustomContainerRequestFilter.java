package io.quarkus.rest.test.customproviders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.quarkus.rest.ContainerRequestFilter;

public class CustomContainerRequestFilter {

    @ContainerRequestFilter
    public void whatever(UriInfo uriInfo, HttpHeaders httpHeaders, ContainerRequestContext requestContext) {
        String customHeaderValue = uriInfo.getPath() + "-" + httpHeaders.getHeaderString("some-input");
        requestContext.getHeaders().putSingle("custom-header", customHeaderValue);
    }
}
