package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.interceptor.InvocationContext;

public class TerminalInvocationContext extends AroundInvokeInvocationContext {

    public TerminalInvocationContext(Object target, Method method, Function<InvocationContext, Object> aroundInvokeForward,
            Object[] parameters,
            Map<String, Object> contextData,
            Set<Annotation> interceptorBindings) {
        super(target, method, aroundInvokeForward, parameters,
                (contextData == null) ? null : new HashMap<String, Object>(contextData),
                interceptorBindings);
    }

    public TerminalInvocationContext(NonTerminalInvocationContext ctx) {
        super(ctx.getTarget(), ctx.getMethod(), ctx.getAroundInvokeForward(), ctx.getParameters(), ctx.contextData,
                ctx.getInterceptorBindings());
    }

    @Override
    public Object proceedInternal() throws Exception {
        return getAroundInvokeForward().apply(this);
    }

}
