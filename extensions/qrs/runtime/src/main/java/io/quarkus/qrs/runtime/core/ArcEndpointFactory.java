package io.quarkus.qrs.runtime.core;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qrs.runtime.spi.EndpointFactory;

public class ArcEndpointFactory implements EndpointFactory {

    private final BeanContainer.Factory<?> factory;

    public ArcEndpointFactory(Class<?> target, BeanContainer beanContainer) {
        factory = beanContainer.instanceFactory(target);
    }

    @Override
    public EndpointInstance createInstance() {
        BeanContainer.Instance<?> instance = factory.create();
        return new EndpointInstance() {
            @Override
            public Object getInstance() {
                return instance.get();
            }

            @Override
            public void close() {
                instance.close();
            }
        };
    }

    @Override
    public EndpointInstance createInstance(RequestContext requestContext) {
        return createInstance();
    }

    public static class Supplier {

    }
}
