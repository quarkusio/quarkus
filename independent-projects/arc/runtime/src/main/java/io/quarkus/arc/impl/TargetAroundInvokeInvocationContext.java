package io.quarkus.arc.impl;

import java.util.List;
import java.util.function.BiFunction;

import jakarta.interceptor.InvocationContext;

/**
 * A special {@link jakarta.interceptor.InvocationContext} that is used for around invoke methods declared in a hierarchy of a
 * target class.
 * <p>
 * The interceptor methods defined by the superclasses are invoked before the interceptor method defined by the interceptor
 * class, most general superclass first.
 */
class TargetAroundInvokeInvocationContext extends InnerInvocationContext {

    static Object perform(InvocationContext delegate,
            List<BiFunction<Object, InvocationContext, Object>> methods,
            BiFunction<Object, InvocationContext, Object> aroundInvokeForward)
            throws Exception {
        return methods.get(0).apply(delegate.getTarget(),
                new TargetAroundInvokeInvocationContext(delegate, methods, aroundInvokeForward));
    }

    private final List<BiFunction<Object, InvocationContext, Object>> methods;
    private final BiFunction<Object, InvocationContext, Object> aroundInvokeForward;

    TargetAroundInvokeInvocationContext(InvocationContext delegate, List<BiFunction<Object, InvocationContext, Object>> methods,
            BiFunction<Object, InvocationContext, Object> aroundInvokeForward) {
        super(delegate, delegate.getParameters());
        this.methods = methods;
        this.aroundInvokeForward = aroundInvokeForward;
    }

    protected Object proceed(int currentPosition) throws Exception {
        try {
            if (currentPosition < methods.size()) {
                // Invoke the next interceptor method from the hierarchy
                return methods.get(currentPosition)
                        .apply(delegate.getTarget(),
                                new NextInnerInvocationContext(currentPosition + 1, parameters));
            } else {
                // Invoke the target method
                return aroundInvokeForward.apply(delegate.getTarget(), this);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();

            // e.getCause() may return null
            if (cause == null) {
                cause = e;
            }
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
