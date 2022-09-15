package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;

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
