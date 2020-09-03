package io.quarkus.rest.runtime.handlers;

import java.util.Collection;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerResponseContext;

public class ResourceResponseInterceptorHandler implements RestHandler {

    private final Collection<ContainerResponseFilter> filters;

    public ResourceResponseInterceptorHandler(Collection<ContainerResponseFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        QuarkusRestContainerRequestContext filterRequestContext = requestContext.getContainerRequestContext();
        filterRequestContext.setResponse(true);
        filterRequestContext.setPreMatch(false);
        ContainerResponseContext filterResponseContext = new QuarkusRestContainerResponseContext(requestContext);
        for (ContainerResponseFilter interceptor : filters) {
            interceptor.filter(filterRequestContext, filterResponseContext);
        }
    }
}
