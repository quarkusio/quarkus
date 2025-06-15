package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

@WithTransaction
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class WithTransactionInterceptor extends AbstractUniInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Bindings are validated at build time - method-level binding declared on a method that does not return Uni
        // results in a build failure
        // However, a class-level binding implies that methods that do not return Uni are just a no-op
        if (isUniReturnType(context)) {
            return SessionOperations.withTransaction(() -> proceedUni(context));
        }
        return context.proceed();
    }

}
