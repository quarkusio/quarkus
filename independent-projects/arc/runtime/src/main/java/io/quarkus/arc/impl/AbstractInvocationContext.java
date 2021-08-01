package io.quarkus.arc.impl;

import io.quarkus.arc.ArcInvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

abstract class AbstractInvocationContext implements ArcInvocationContext, Supplier<Map<String, Object>> {

    private static final Object[] EMPTY_PARAMS = new Object[0];

    protected final Method method;
    protected final Constructor<?> constructor;
    protected final Set<Annotation> interceptorBindings;
    protected final List<InterceptorInvocation> chain;
    protected Object target;
    protected Object[] parameters;
    // The map is initialized lazily but we need to use a holder so that all interceptors in the chain can access the same data
    protected LazyValue<Map<String, Object>> contextData;

    protected AbstractInvocationContext(Object target, Method method,
            Constructor<?> constructor,
            Object[] parameters, LazyValue<Map<String, Object>> contextData,
            Set<Annotation> interceptorBindings, List<InterceptorInvocation> chain) {
        this.target = target;
        this.method = method;
        this.constructor = constructor;
        this.parameters = parameters != null ? parameters : EMPTY_PARAMS;
        this.contextData = contextData != null ? contextData : new LazyValue<>(this);
        this.interceptorBindings = Collections.unmodifiableSet(interceptorBindings);
        this.chain = chain;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData.get();
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return interceptorBindings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
        for (Annotation annotation : interceptorBindings) {
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
        for (Annotation annotation : (Set<Annotation>) interceptorBindings) {
            if (annotation.annotationType().equals(annotationType)) {
                found.add((T) annotation);
            }
        }
        return found;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        validateParameters(params);
        this.parameters = params;
    }

    protected void validateParameters(Object[] params) {
        int newParametersCount = Objects.requireNonNull(params).length;
        Class<?>[] parameterTypes = method.getParameterTypes();
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
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public Map<String, Object> get() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS, interceptorBindings);
        return result;
    }

}
