package io.quarkus.rest.runtime.jaxrs;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.runtime.model.ResourceInterceptors;
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.spi.BeanFactory;

// TODO: It might not make sense to have this extend from QuarkusRestFeatureContext
public class QuarkusRestDynamicFeatureContext extends QuarkusRestFeatureContext {

    public QuarkusRestDynamicFeatureContext(ResourceInterceptors interceptors, QuarkusRestConfiguration configuration,
            BeanContainer beanContainer) {
        super(interceptors, null, configuration, beanContainer);
    }

    @Override
    protected boolean isAllowed(Class<?> componentClass) {
        // For the time being we only support filters
        return isFilter(componentClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void registerFilters(Class<?> componentClass, BeanFactory<?> beanFactory, Integer priority) {
        boolean isRequest = ContainerRequestFilter.class.isAssignableFrom(componentClass);
        boolean isResponse = ContainerResponseFilter.class.isAssignableFrom(componentClass);
        if (isRequest) {
            ResourceRequestInterceptor requestInterceptor = new ResourceRequestInterceptor();
            setFilterPriority(componentClass, priority, requestInterceptor);
            requestInterceptor.setFactory(getFactory(componentClass, beanFactory));
            interceptors.addGlobalRequestInterceptor(requestInterceptor);
        }
        if (isResponse) {
            ResourceResponseInterceptor responseInterceptor = new ResourceResponseInterceptor();
            setFilterPriority(componentClass, priority, responseInterceptor);
            responseInterceptor.setFactory(getFactory(componentClass, beanFactory));
            interceptors.addGlobalResponseInterceptor(responseInterceptor);
        }
    }
}
