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

    private final Supplier<Object> aroundConstructForward;

    AroundConstructInvocationContext(Constructor<?> constructor, Object[] parameters, Set<Annotation> interceptorBindings,
            List<InterceptorInvocation> chain, Supplier<Object> aroundConstructForward) {
        super(null, constructor, parameters, interceptorBindings, chain);
        this.aroundConstructForward = aroundConstructForward;
    }

    protected void interceptorChainCompleted() throws Exception {
        target = aroundConstructForward.get();
    }

}
