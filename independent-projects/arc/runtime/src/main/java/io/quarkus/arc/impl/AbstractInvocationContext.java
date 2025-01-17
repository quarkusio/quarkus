package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.quarkus.arc.ArcInvocationContext;

abstract class AbstractInvocationContext implements ArcInvocationContext {

    private static final Object[] EMPTY_PARAMS = new Object[0];

    protected Object target;
    protected Object[] parameters;
    protected ContextDataMap contextData;

    protected AbstractInvocationContext(Object target, Object[] parameters, ContextDataMap contextData) {
        this.target = target;
        this.parameters = parameters != null ? parameters : EMPTY_PARAMS;
        this.contextData = contextData;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
        for (Annotation annotation : getInterceptorBindings()) {
            if (annotation.annotationType().equals(annotationType)) {
                return (T) annotation;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType) {
        List<T> found = new ArrayList<>();
        for (Annotation annotation : getInterceptorBindings()) {
            if (annotation.annotationType().equals(annotationType)) {
                found.add((T) annotation);
            }
        }
        return found;
    }

    static void validateParameters(Executable executable, Object[] params) {
        int newParametersCount = Objects.requireNonNull(params).length;
        Class<?>[] parameterTypes = executable.getParameterTypes();
        if (parameterTypes.length != newParametersCount) {
            throw new IllegalArgumentException(
                    "Wrong number of parameters - method has " + Arrays.toString(parameterTypes) + ", attempting to set "
                            + Arrays.toString(params));
        }
        for (int i = 0; i < params.length; i++) {
            if (parameterTypes[i].isPrimitive() && params[i] == null) {
                throw new IllegalArgumentException("Trying to set a null value to a primitive parameter [position: " + i
                        + ", type: " + parameterTypes[i] + "]");
            }
            if (params[i] != null) {
                if (!Types.boxedClass(parameterTypes[i]).isAssignableFrom(Types.boxedClass(params[i].getClass()))) {
                    throw new IllegalArgumentException("The parameter type [" + params[i].getClass()
                            + "] can not be assigned to the type for the target method [" + parameterTypes[i] + "]");
                }
            }
        }
    }

    @Override
    public Method getMethod() {
        return null;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

}
