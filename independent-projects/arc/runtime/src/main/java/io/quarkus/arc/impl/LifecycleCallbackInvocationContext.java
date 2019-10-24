package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

/**
 * A simple InvocationContext implementation used for PostConstruct and PreDestroy callbacks.
 * <p>
 * All lifecycle callback interceptors of a specific chain must be invoked on the same thread.
 */
class LifecycleCallbackInvocationContext extends AbstractInvocationContext {

    private int position = 0;

    LifecycleCallbackInvocationContext(Object target, Constructor<?> constructor, Set<Annotation> interceptorBindings,
            List<InterceptorInvocation> chain) {
        super(target, null, constructor, null, null, interceptorBindings, chain);
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (position < chain.size()) {
                // Invoke the next interceptor in the chain
                invokeNext();
            } else {
                // The invocation of proceed in the last interceptor method in the chain
                interceptorChainCompleted();
            }
            // The return value of a lifecycle callback is ignored
            return null;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    protected void interceptorChainCompleted() throws Exception {
        // No-op
    }

    protected Object invokeNext() throws Exception {
        try {
            return chain.get(position++).invoke(this);
        } finally {
            position--;
        }
    }

}
