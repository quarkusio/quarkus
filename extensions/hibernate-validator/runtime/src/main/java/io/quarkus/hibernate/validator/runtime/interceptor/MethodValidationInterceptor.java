package io.quarkus.hibernate.validator.runtime.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
