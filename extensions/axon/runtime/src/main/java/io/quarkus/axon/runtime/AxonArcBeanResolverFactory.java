package io.quarkus.axon.runtime;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import javax.enterprise.context.ApplicationScoped;

import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.ParameterResolver;
import org.axonframework.messaging.annotation.ParameterResolverFactory;

import io.quarkus.arc.runtime.BeanContainer;

@ApplicationScoped
public class AxonArcBeanResolverFactory implements ParameterResolverFactory {

    private static BeanContainer beanContainer;

    public AxonArcBeanResolverFactory() {
    }

    // This class will be loaded by the Java service loader and there is no dependency injection possible (?)
    // so the beanContainer should be set in a static way.
    static void setBeanContainer(BeanContainer beanContainer) {
        AxonArcBeanResolverFactory.beanContainer = beanContainer;
    }

    @Override
    public ParameterResolver createInstance(Executable executable, Parameter[] parameters, int parameterIndex) {
        // Ignore the first index is not the best choice. Maybe this could be smarter?
        if (beanContainer == null || parameterIndex == 0) {
            return null;
        }

        Class<?> parameterType = parameters[parameterIndex].getType();
        Object instance = beanContainer.instance(parameterType);
        if (instance != null) {
            return new AxonArcBeanResolver(instance);
        } else {
            return null;
        }
    }

    private static class AxonArcBeanResolver implements ParameterResolver<Object> {
        private final Object bean;

        AxonArcBeanResolver(Object bean) {
            this.bean = bean;
        }

        @Override
        public Object resolveParameterValue(Message message) {
            return bean;
        }

        @Override
        public boolean matches(Message message) {
            return true;
        }
    }
}
