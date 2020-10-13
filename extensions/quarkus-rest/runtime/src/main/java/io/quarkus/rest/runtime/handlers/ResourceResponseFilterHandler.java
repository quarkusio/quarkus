package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerRequestContextImpl;

public class ResourceResponseFilterHandler implements ServerRestHandler {

    private final ContainerResponseFilter filter;

    public ResourceResponseFilterHandler(ContainerResponseFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.requireCDIRequestScope();
        QuarkusRestContainerRequestContextImpl filterRequestContext = requestContext.getContainerRequestContext();
        filterRequestContext.setResponse(true);
        filterRequestContext.setPreMatch(false);
        ContainerResponseContext filterResponseContext = requestContext.getContainerResponseContext();
        filter.filter(filterRequestContext, filterResponseContext);
    }
}
