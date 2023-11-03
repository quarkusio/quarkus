package org.jboss.resteasy.reactive.server.core.reflection;

import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ReflectionConstructorParameterConverterSupplier implements ParameterConverterSupplier {

    private String targetTypeName;

    public ReflectionConstructorParameterConverterSupplier() {
    }

    public ReflectionConstructorParameterConverterSupplier(String targetTypeName) {
        this.targetTypeName = targetTypeName;
    }

    @Override
    public ParameterConverter get() {
        try {
            return new ReflectionConstructorParameterConverter(Thread.currentThread().getContextClassLoader()
                    .loadClass(targetTypeName).getDeclaredConstructor(String.class));
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClassName() {
        return ReflectionValueOfParameterConverter.class.getName();
    }

    public String getTargetTypeName() {
        return targetTypeName;
    }

    public ReflectionConstructorParameterConverterSupplier setTargetTypeName(String targetTypeName) {
        this.targetTypeName = targetTypeName;
        return this;
    }

}
