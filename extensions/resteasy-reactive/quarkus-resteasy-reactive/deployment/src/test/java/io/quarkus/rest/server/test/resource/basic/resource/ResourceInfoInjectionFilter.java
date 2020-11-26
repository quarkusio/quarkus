package io.quarkus.rest.server.test.resource.basic.resource;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceInfoInjectionFilter implements ContainerResponseFilter {
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            responseContext.setStatus(responseContext.getStatus() * 2);
        } else {
            responseContext.setEntity(method.getName(), null, MediaType.TEXT_PLAIN_TYPE);
        }
    }
}
