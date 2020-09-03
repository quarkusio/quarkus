package io.quarkus.rest.runtime.handlers;

import java.util.Collection;

import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerRequestContext;

public class ResourceRequestInterceptorHandler implements RestHandler {

    private final Collection<ContainerRequestFilter> filters;
    private final boolean preMatch;

    public ResourceRequestInterceptorHandler(Collection<ContainerRequestFilter> filters, boolean preMatch) {
        this.filters = filters;
        this.preMatch = preMatch;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        QuarkusRestContainerRequestContext filterContext = requestContext.getContainerRequestContext();
        filterContext.setPreMatch(preMatch);
        for (ContainerRequestFilter interceptor : filters) {
            interceptor.filter(filterContext);
            if (filterContext.isAborted()) {
                return;
            }
        }
    }
}
