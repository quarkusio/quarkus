package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.interceptor.InvocationContext;

public class NonTerminalInvocationContext extends AroundInvokeInvocationContext {

    private final int position;
    private final List<InvocationContextImpl.InterceptorInvocation> chain;

    public NonTerminalInvocationContext(Object target, Method method, Function<InvocationContext, Object> aroundInvokeForward,
            Object[] parameters,
            Set<Annotation> interceptorBindings,
            List<InvocationContextImpl.InterceptorInvocation> chain) {
        this(target, method, aroundInvokeForward, parameters, newContextData(interceptorBindings), interceptorBindings, 0,
                chain);
    }

    public NonTerminalInvocationContext(NonTerminalInvocationContext ctx) {
        this(ctx.getTarget(), ctx.getMethod(), ctx.getAroundInvokeForward(), ctx.getParameters(), ctx.contextData,
                ctx.getInterceptorBindings(), ctx.position + 1,
                ctx.chain);
    }

    private NonTerminalInvocationContext(Object target, Method method, Function<InvocationContext, Object> aroundInvokeForward,
            Object[] parameters,
            Map<String, Object> contextData,
            Set<Annotation> interceptorBindings, int position, List<InvocationContextImpl.InterceptorInvocation> chain) {
        super(target, method, aroundInvokeForward, parameters, contextData, interceptorBindings);
        this.position = position;
        this.chain = chain;
    }

    @Override
    public Object proceedInternal() throws Exception {
        AbstractInvocationContext ctx = createNextContext();
        return chain.get(position).invoke(ctx);
    }

    private AbstractInvocationContext createNextContext() {
        if (position + 1 == chain.size()) {
            return new TerminalInvocationContext(this);
        } else {
            return new NonTerminalInvocationContext(this);
        }
    }
}
