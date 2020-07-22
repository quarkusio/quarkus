package io.quarkus.qrs.runtime.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerResponseContext;
import io.quarkus.qrs.runtime.model.ResourceResponseInterceptor;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.quarkus.qrs.runtime.spi.BeanFactory.BeanInstance;

public class ResourceResponseInterceptorHandler implements RestHandler {

    private List<BeanFactory<ContainerResponseFilter>> interceptors;

    public ResourceResponseInterceptorHandler(List<ResourceResponseInterceptor> requestInterceptors) {
        this.interceptors = new ArrayList<>(requestInterceptors.size());
        for (ResourceResponseInterceptor interceptor : requestInterceptors) {
            interceptors.add(interceptor.getFactory());
        }
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        // FIXME: reuse previous request context?
        ContainerRequestContext filterRequestContext = new QrsContainerRequestContext(requestContext);
        ContainerResponseContext filterResponseContext = new QrsContainerResponseContext(requestContext);
        for (BeanFactory<ContainerResponseFilter> interceptor : interceptors) {
            BeanInstance<ContainerResponseFilter> instance = interceptor.createInstance(requestContext);
            instance.getInstance().filter(filterRequestContext, filterResponseContext);
        }
    }

}
