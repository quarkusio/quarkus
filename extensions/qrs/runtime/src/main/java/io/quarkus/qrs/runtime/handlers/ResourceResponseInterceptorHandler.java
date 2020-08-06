package io.quarkus.qrs.runtime.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerResponseContext;
import io.quarkus.qrs.runtime.model.ResourceResponseInterceptor;
import io.quarkus.qrs.runtime.spi.BeanFactory.BeanInstance;
import io.quarkus.runtime.ShutdownContext;

public class ResourceResponseInterceptorHandler implements RestHandler, Closeable {

    private final ContainerResponseFilter[] filters;
    private List<BeanInstance<ContainerResponseFilter>> interceptors;

    public ResourceResponseInterceptorHandler(List<ResourceResponseInterceptor> responseInterceptors,
            ShutdownContext shutdownContext) {
        this.interceptors = new ArrayList<>(responseInterceptors.size());
        this.filters = new ContainerResponseFilter[responseInterceptors.size()];
        for (int i = 0; i < responseInterceptors.size(); i++) {
            ResourceResponseInterceptor interceptor = responseInterceptors.get(i);
            BeanInstance<ContainerResponseFilter> instance = interceptor.getFactory().createInstance();
            filters[i] = instance.getInstance();
            interceptors.add(instance);
        }
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(this));
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        QrsContainerRequestContext filterRequestContext = requestContext.getContainerRequestContext();
        filterRequestContext.setResponse(true);
        filterRequestContext.setPreMatch(false);
        ContainerResponseContext filterResponseContext = new QrsContainerResponseContext(requestContext);
        for (ContainerResponseFilter interceptor : filters) {
            interceptor.filter(filterRequestContext, filterResponseContext);
        }
    }

    @Override
    public void close() throws IOException {
        for (BeanInstance<ContainerResponseFilter> i : interceptors) {
            i.close();
        }

    }
}
