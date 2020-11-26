package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.function.Function;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import org.jboss.resteasy.reactive.common.jaxrs.QuarkusRestConfiguration;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.spi.BeanFactory;

// TODO: It might not make sense to have this extend from QuarkusRestFeatureContext
public class DynamicFeatureContext extends FeatureContextImpl {

    public DynamicFeatureContext(ResourceInterceptors interceptors, QuarkusRestConfiguration configuration,
            Function<Class<?>, BeanFactory<?>> beanContainer) {
        super(interceptors, null, configuration, beanContainer);
    }

    @Override
    protected boolean isAllowed(Class<?> componentClass) {
        // For the time being we only support filters and interceptors
        return isFilter(componentClass) || isInterceptor(componentClass);
    }

    @Override
    protected void registerFilters(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority) {
        boolean isRequest = ContainerRequestFilter.class.isAssignableFrom(componentClass);
        boolean isResponse = ContainerResponseFilter.class.isAssignableFrom(componentClass);
        if (isRequest) {
            register(componentClass, beanFactory, priority, this.interceptors.getContainerRequestFilters());
        }
        if (isResponse) {
            register(componentClass, beanFactory, priority, this.interceptors.getContainerResponseFilters());
        }
    }

    private <T> void register(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority,
            InterceptorContainer<T> interceptors) {
        ResourceInterceptor<T> responseInterceptor = interceptors.create();
        setFilterPriority(componentClass, priority, responseInterceptor);
        responseInterceptor.setFactory(getFactory(componentClass, beanFactory));
        interceptors.addGlobalRequestInterceptor(responseInterceptor);
    }
}
