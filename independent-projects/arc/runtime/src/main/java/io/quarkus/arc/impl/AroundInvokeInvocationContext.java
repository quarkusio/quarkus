package io.quarkus.arc.impl;

import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Special type of InvocationContext for AroundInvoke interceptors.
 * <p>
 * A new instance of {@link AroundInvokeInvocationContext} is created for each interceptor in the chain. This does not comply
 * with the spec but allows for "asynchronous continuation" of an interceptor chain execution. In other words, it is possible to
 * "cut off" the chain (interceptors executed before dispatch return immediately) and execute all remaining interceptors
 * asynchronously, possibly on a different thread.
 * <p>
 * Note that context data and method parameters are mutable and are not guarded/synchronized. We expect them to be modified
 * before or after dispatch. If modified before and after dispatch an unpredicatble behavior may occur.
 */
class AroundInvokeInvocationContext extends AbstractInvocationContext {

    private final int position;
    private final Function<InvocationContext, Object> aroundInvokeForward;

    AroundInvokeInvocationContext(Object target, Method method, Object[] parameters,
            ContextDataMap contextData, Set<Annotation> interceptorBindings, int position,
            List<InterceptorInvocation> chain, Function<InvocationContext, Object> aroundInvokeForward) {
        super(target, method, null, parameters, contextData, interceptorBindings, chain);
        this.position = position;
        this.aroundInvokeForward = aroundInvokeForward;
    }

    static Object perform(Object target, Method method,
            Function<InvocationContext, Object> aroundInvokeForward, Object[] parameters,
            List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings) throws Exception {

        return chain.get(0).invoke(new AroundInvokeInvocationContext(target, method,
                parameters, null, interceptorBindings, 1, chain, aroundInvokeForward));
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (position < chain.size()) {
                // Invoke the next interceptor in the chain
                return chain.get(position).invoke(new AroundInvokeInvocationContext(target, method,
                        parameters, contextData, interceptorBindings, position + 1, chain, aroundInvokeForward));
            } else {
                // Invoke the target method
                return aroundInvokeForward.apply(this);
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
