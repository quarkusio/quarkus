package io.quarkus.rest.server.runtime.handlers;

import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestContainerRequestContextImpl;

public class ResourceRequestFilterHandler implements ServerRestHandler {

    private final ContainerRequestFilter filter;
    private final boolean preMatch;

    public ResourceRequestFilterHandler(ContainerRequestFilter filter, boolean preMatch) {
        this.filter = filter;
        this.preMatch = preMatch;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        QuarkusRestContainerRequestContextImpl filterContext = requestContext.getContainerRequestContext();
        if (filterContext.isAborted()) {
            return;
        }
        requestContext.requireCDIRequestScope();
        filterContext.setPreMatch(preMatch);
        filter.filter(filterContext);
    }
}
