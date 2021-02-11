package org.jboss.resteasy.reactive.server.handlers;

import javax.ws.rs.container.ContainerRequestFilter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ResourceRequestFilterHandler implements ServerRestHandler {

    private final ContainerRequestFilter filter;
    private final boolean preMatch;

    public ResourceRequestFilterHandler(ContainerRequestFilter filter, boolean preMatch) {
        this.filter = filter;
        this.preMatch = preMatch;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        ContainerRequestContextImpl filterContext = requestContext.getContainerRequestContext();
        if (filterContext.isAborted()) {
            return;
        }
        requestContext.requireCDIRequestScope();
        filterContext.setPreMatch(preMatch);
        filter.filter(filterContext);
    }
}
