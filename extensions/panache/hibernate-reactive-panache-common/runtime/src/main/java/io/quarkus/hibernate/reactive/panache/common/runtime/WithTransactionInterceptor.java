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
        // Bindings are validated at build time - method-level binding declared on a method that does not return Uni results in a build failure
        // However, a class-level binding implies that methods that do not return Uni are just a no-op
        if (isUniReturnType(context)) {
            WithTransaction withTransaction = getAnnotation(context);
            String persistenceUnitName = withTransaction.value();
            if (withTransaction.stateless()) {
                return SessionOperations.withStatelessTransaction(persistenceUnitName, () -> proceedUni(context));
            } else {
                return SessionOperations.withTransaction(persistenceUnitName, () -> proceedUni(context));
            }
        }
        return context.proceed();
    }

    private WithTransaction getAnnotation(InvocationContext context) {
        WithTransaction annotation = context.getMethod().getAnnotation(WithTransaction.class);
        if (annotation == null) {
            // Check class-level annotation
            annotation = context.getTarget().getClass().getAnnotation(WithTransaction.class);
        }
        return annotation;
    }

}
