package io.quarkus.hibernate.validator.runtime.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@MethodValidated
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 800)
public class MethodValidationInterceptor extends AbstractMethodValidationInterceptor {

    @AroundInvoke
    @Override
    public Object validateMethodInvocation(InvocationContext ctx) throws Exception {
        return super.validateMethodInvocation(ctx);
    }

    @AroundConstruct
    @Override
    public void validateConstructorInvocation(InvocationContext ctx) throws Exception {
        super.validateConstructorInvocation(ctx);
    }
}
