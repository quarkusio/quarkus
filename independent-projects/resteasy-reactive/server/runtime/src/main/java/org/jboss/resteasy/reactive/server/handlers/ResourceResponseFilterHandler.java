package org.jboss.resteasy.reactive.server.handlers;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ResourceResponseFilterHandler implements ServerRestHandler {

    private final ContainerResponseFilter filter;

    public ResourceResponseFilterHandler(ContainerResponseFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.requireCDIRequestScope();
        QuarkusRestContainerRequestContextImpl filterRequestContext = requestContext.getContainerRequestContext();
        filterRequestContext.setResponse(true);
        filterRequestContext.setPreMatch(false);
        ContainerResponseContext filterResponseContext = requestContext.getContainerResponseContext();
        filter.filter(filterRequestContext, filterResponseContext);
    }
}
