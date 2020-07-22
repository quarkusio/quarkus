package io.quarkus.qrs.runtime.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerRequestContext;
import io.quarkus.qrs.runtime.model.ResourceRequestInterceptor;
import io.quarkus.qrs.runtime.spi.EndpointFactory;
import io.quarkus.qrs.runtime.spi.EndpointFactory.EndpointInstance;

public class ResourceRequestInterceptorHandler implements RestHandler {

    private List<EndpointFactory> interceptors;

    public ResourceRequestInterceptorHandler(List<ResourceRequestInterceptor> requestInterceptors) {
        this.interceptors = new ArrayList<>(requestInterceptors.size());
        for (ResourceRequestInterceptor interceptor : requestInterceptors) {
            interceptors.add(interceptor.getFactory());
        }
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        ContainerRequestContext filterContext = new QrsContainerRequestContext(requestContext);
        for (EndpointFactory interceptor : interceptors) {
            EndpointInstance instance = interceptor.createInstance(requestContext);
            ((ContainerRequestFilter) instance.getInstance()).filter(filterContext);
            // FIXME: check if aborted
        }
    }

}
