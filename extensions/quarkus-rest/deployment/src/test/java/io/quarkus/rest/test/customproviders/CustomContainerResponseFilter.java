package io.quarkus.rest.test.customproviders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import io.quarkus.rest.ContainerResponseFilter;
import io.quarkus.rest.runtime.spi.SimplifiedResourceInfo;

public class CustomContainerResponseFilter {

    @ContainerResponseFilter
    public void whatever(SimplifiedResourceInfo simplifiedResourceInfo, ContainerResponseContext responseContext,
            ContainerRequestContext requestContext) {
        responseContext.getHeaders().putSingle("java-method", simplifiedResourceInfo.getMethodName());
    }
}
