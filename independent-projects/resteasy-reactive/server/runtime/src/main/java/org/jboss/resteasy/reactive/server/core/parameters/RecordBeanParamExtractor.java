package org.jboss.resteasy.reactive.server.core.parameters;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionContext;

public class RecordBeanParamExtractor implements ParameterExtractor {

    private final MethodHandle factoryMethod;

    public RecordBeanParamExtractor(Class<?> target) {
        try {
            factoryMethod = MethodHandles.lookup().findStatic(target, "__quarkus_rest_inject",
                    MethodType.methodType(target, ResteasyReactiveInjectionContext.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find target generated factory method on record @BeanParam type", e);
        }
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        try {
            return factoryMethod.invoke(context);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to invoke generated factory method on record @BeanParam type", e);
        }
    }
}
