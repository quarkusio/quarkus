package io.quarkus.arc.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.BiFunction;

import jakarta.interceptor.InvocationContext;

/**
 * A special {@link jakarta.interceptor.InvocationContext} that is used if multiple interceptor methods are declared in a
 * hierarchy of an interceptor class.
 * <p>
 * The interceptor methods defined by the superclasses are invoked before the interceptor method defined by the interceptor
 * class, most general superclass first.
 */
class SuperclassInvocationContext extends InnerInvocationContext {

    static Object perform(InvocationContext delegate, List<BiFunction<Object, InvocationContext, Object>> methods,
            Object interceptorInstance, Object[] parameters)
            throws Exception {
        return methods.get(0).apply(interceptorInstance,
                new SuperclassInvocationContext(delegate, methods, interceptorInstance, parameters));
    }

    private final Object interceptorInstance;
    private final List<BiFunction<Object, InvocationContext, Object>> methods;

    SuperclassInvocationContext(InvocationContext delegate, List<BiFunction<Object, InvocationContext, Object>> methods,
            Object interceptorInstance, Object[] parameters) {
        super(delegate, parameters);
        this.methods = methods;
        this.interceptorInstance = interceptorInstance;
    }

    @Override
    protected Object proceed(int currentPosition) throws Exception {
        try {
            if (currentPosition < methods.size()) {
                // Invoke the next interceptor method from the hierarchy
                return methods.get(currentPosition)
                        .apply(interceptorInstance,
                                new NextInnerInvocationContext(currentPosition + 1, parameters));
            } else {
                // Invoke the next interceptor in the chain
                return delegate.proceed();
            }
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

}
