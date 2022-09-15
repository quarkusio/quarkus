package org.jboss.resteasy.reactive.server.handlers;

import jakarta.ws.rs.container.ContainerRequestFilter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ResourceRequestFilterHandler implements ServerRestHandler {

    private final ContainerRequestFilter filter;
    private final boolean preMatch;
    private final boolean nonBlockingRequired;
    private final boolean readBody;

    public ResourceRequestFilterHandler(ContainerRequestFilter filter, boolean preMatch, boolean nonBlockingRequired,
            boolean readBody) {
        this.filter = filter;
        this.preMatch = preMatch;
        this.nonBlockingRequired = nonBlockingRequired;
        this.readBody = readBody;
    }

    public ContainerRequestFilter getFilter() {
        return filter;
    }

    public boolean isPreMatch() {
        return preMatch;
    }

    public boolean isNonBlockingRequired() {
        return nonBlockingRequired;
    }

    public boolean isReadBody() {
        return readBody;
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
