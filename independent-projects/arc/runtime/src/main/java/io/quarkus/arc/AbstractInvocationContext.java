package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.interceptor.InvocationContext;

public abstract class AbstractInvocationContext implements InvocationContext {

    protected Map<String, Object> contextData;
    protected final Method method;
    protected Object[] parameters;
    protected final Object target;
    protected final Object timer;
    protected final Constructor<?> constructor;
    protected final Set<Annotation> interceptorBindings;
    protected Function<InvocationContext, Object> aroundInvokeForward;

    protected AbstractInvocationContext(Object target, Method method, Function<InvocationContext, Object> aroundInvokeForward,
            Object[] parameters,
            Map<String, Object> contextData, Set<Annotation> interceptorBindings) {
        this(target, method, aroundInvokeForward, null, parameters, null, contextData, interceptorBindings);
    }

    protected AbstractInvocationContext(Object target, Method method, Function<InvocationContext, Object> aroundInvokeForward,
            Constructor<?> constructor,
            Object[] parameters, Object timer, Map<String, Object> contextData,
            Set<Annotation> interceptorBindings) {
        this.target = target;
        this.method = method;
        this.aroundInvokeForward = aroundInvokeForward;
        this.constructor = constructor;
        this.parameters = parameters;
        this.timer = timer;
        this.contextData = contextData;
        this.interceptorBindings = interceptorBindings;
    }

    @Override
    public Map<String, Object> getContextData() {
        if (contextData == null) {
            contextData = newContextData(interceptorBindings);
        }
        return contextData;
    }

    public Set<Annotation> getInterceptorBindings() {
        return interceptorBindings;
    }

    @Override
    public abstract Object proceed() throws Exception;

    protected static Map<String, Object> newContextData(Set<Annotation> interceptorBindings) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS, interceptorBindings);
        return result;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        if (parameters == null) {
            throw new IllegalStateException();
        }
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) throws IllegalStateException, IllegalArgumentException {
        if (parameters == null) {
            throw new IllegalStateException();
        }
        this.parameters = params;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        return timer;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    protected Function<InvocationContext, Object> getAroundInvokeForward() {
        return aroundInvokeForward;
    }

    abstract Object proceedInternal() throws Exception;

}
