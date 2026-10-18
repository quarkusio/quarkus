package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * An {@code InvocationContext} implementation used for {@code PostConstruct} and {@code PreDestroy} callbacks.
 */
class PostConstructPreDestroyInvocationContext extends LifecycleCallbackInvocationContext {
    private final Runnable forward;

    PostConstructPreDestroyInvocationContext(Object target, Object[] parameters,
            Set<Annotation> bindings, List<InterceptorInvocation> chain, Runnable forward) {
        super(target, parameters, bindings, chain);
        this.forward = forward;
    }

    @Override
    protected void interceptorChainCompleted() {
        if (forward != null) {
            forward.run();
        }
    }

    @Override
    public Object[] getParameters() {
        throw new IllegalStateException();
    }

    @Override
    public void setParameters(Object[] params) {
        throw new IllegalStateException();
    }
}
