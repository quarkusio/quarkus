package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * An {@code InvocationContext} implementation used for {@code AroundConstruct} callbacks.
 */
class AroundConstructInvocationContext extends LifecycleCallbackInvocationContext {

    private final Constructor<?> constructor;
    private final Function<Object[], Object> forward;

    AroundConstructInvocationContext(Constructor<?> constructor, Object[] parameters, Set<Annotation> interceptorBindings,
            List<InterceptorInvocation> chain, Function<Object[], Object> forward) {
        super(null, parameters, interceptorBindings, chain);
        this.forward = forward;
        this.constructor = constructor;
    }

    @Override
    protected void interceptorChainCompleted() {
        target = forward.apply(parameters);
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        validateParameters(constructor, params);
        this.parameters = params;
    }

}
