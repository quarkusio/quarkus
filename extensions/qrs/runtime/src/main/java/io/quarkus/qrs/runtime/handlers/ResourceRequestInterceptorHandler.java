package io.quarkus.qrs.runtime.handlers;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerRequestContext;
import io.quarkus.qrs.runtime.model.ResourceRequestInterceptor;
import io.quarkus.qrs.runtime.spi.BeanFactory.BeanInstance;
import io.quarkus.runtime.ShutdownContext;

public class ResourceRequestInterceptorHandler implements RestHandler, Closeable {

    private ContainerRequestFilter[] interceptors;
    private List<BeanInstance<?>> instances;
    private final boolean preMatch;

    public ResourceRequestInterceptorHandler(List<ResourceRequestInterceptor> requestInterceptors, ShutdownContext context,
            boolean preMatch) {
        this.interceptors = new ContainerRequestFilter[requestInterceptors.size()];
        this.preMatch = preMatch;
        instances = new ArrayList<>();
        for (int i = 0; i < requestInterceptors.size(); i++) {
            ResourceRequestInterceptor interceptor = requestInterceptors.get(i);
            BeanInstance<ContainerRequestFilter> instance = interceptor.getFactory().createInstance();
            instances.add(instance);
            interceptors[i] = instance.getInstance();
        }
        context.addShutdownTask(new ShutdownContext.CloseRunnable(this));
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        QrsContainerRequestContext filterContext = requestContext.getContainerRequestContext();
        filterContext.setPreMatch(preMatch);
        for (ContainerRequestFilter interceptor : interceptors) {
            interceptor.filter(filterContext);
            if (filterContext.isAborted()) {
                return;
            }
        }
    }

    public void close() {
        for (BeanInstance<?> i : instances) {
            i.close();
        }
    }
}
