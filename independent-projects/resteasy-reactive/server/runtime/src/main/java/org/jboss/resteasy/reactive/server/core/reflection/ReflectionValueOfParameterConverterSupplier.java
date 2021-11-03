package org.jboss.resteasy.reactive.server.core.reflection;

import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;

public class ReflectionValueOfParameterConverterSupplier implements ParameterConverterSupplier {

    private String targetTypeName;
    private String methodName;

    public ReflectionValueOfParameterConverterSupplier() {
    }

    public ReflectionValueOfParameterConverterSupplier(String targetTypeName) {
        this.targetTypeName = targetTypeName;
        this.methodName = "valueOf";
    }

    public ReflectionValueOfParameterConverterSupplier(String targetTypeName, String methodName) {
        this.targetTypeName = targetTypeName;
        this.methodName = methodName;
    }

    @Override
    public ParameterConverter get() {
        try {
            return new ReflectionValueOfParameterConverter(Thread.currentThread().getContextClassLoader()
                    .loadClass(targetTypeName).getDeclaredMethod(methodName, String.class));
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

    public ReflectionValueOfParameterConverterSupplier setTargetTypeName(String targetTypeName) {
        this.targetTypeName = targetTypeName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public ReflectionValueOfParameterConverterSupplier setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }
}
