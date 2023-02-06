package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An InvocationContext implementation used for AroundConstruct callbacks.
 */
class AroundConstructInvocationContext extends LifecycleCallbackInvocationContext {

    private final Constructor<?> constructor;
    private final Supplier<Object> aroundConstructForward;

    AroundConstructInvocationContext(Constructor<?> constructor, Object[] parameters, Set<Annotation> interceptorBindings,
            List<InterceptorInvocation> chain, Supplier<Object> aroundConstructForward) {
        super(null, parameters, interceptorBindings, chain);
        this.aroundConstructForward = aroundConstructForward;
        this.constructor = constructor;
    }

    protected void interceptorChainCompleted() throws Exception {
        target = aroundConstructForward.get();
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
