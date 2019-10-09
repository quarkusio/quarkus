package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.interceptor.InvocationContext;

public abstract class AroundInvokeInvocationContext extends AbstractInvocationContext {

    protected AroundInvokeInvocationContext(Object target, Method method,
            Function<InvocationContext, Object> aroundInvokeForward, Object[] parameters,
            Map<String, Object> contextData, Set<Annotation> interceptorBindings) {
        super(target, method, aroundInvokeForward, parameters, contextData, interceptorBindings);
    }

    protected AroundInvokeInvocationContext(Object target, Method method,
            Function<InvocationContext, Object> aroundInvokeForward, Constructor<?> constructor,
            Object[] parameters, Object timer, Map<String, Object> contextData, Set<Annotation> interceptorBindings) {
        super(target, method, aroundInvokeForward, constructor, parameters, timer, contextData, interceptorBindings);
    }

    public static AbstractInvocationContext create(Object instance, Method method,
            Function<InvocationContext, Object> aroundInvokeForward, Object[] args,
            List<InvocationContextImpl.InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings) {
        if (chain.size() == 0) {
            // terminal invocation context, known to be last context in chain; invoked the original method directly
            return new TerminalInvocationContext(instance, method, aroundInvokeForward, args, null, interceptorBindings);
        } else {
            // non-terminal invocation context, meaning there are more interceptors to be invoked
            return new NonTerminalInvocationContext(instance, method, aroundInvokeForward, args, interceptorBindings, chain);
        }
    }

    @Override
    public Object proceed() throws Exception {
        return proceedInternal();
    }

    @Override
    abstract Object proceedInternal() throws Exception;
}
