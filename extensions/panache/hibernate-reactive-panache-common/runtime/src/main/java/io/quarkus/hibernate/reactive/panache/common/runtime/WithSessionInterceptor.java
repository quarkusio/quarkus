package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.hibernate.reactive.panache.common.WithSession;

@WithSession
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class WithSessionInterceptor extends AbstractUniInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Bindings are validated at build time - method-level binding declared on a method that does not return Uni results in a build failure
        // However, a class-level binding implies that methods that do not return Uni are just a no-op
        if (isUniReturnType(context)) {
            String persistenceUnitName = getPersistenceUnitName(context);
            return SessionOperations.withSession(persistenceUnitName, s -> proceedUni(context));
        }
        return context.proceed();
    }

    private String getPersistenceUnitName(InvocationContext context) {
        // Check method-level annotation first
        WithSession annotation = context.getMethod().getAnnotation(WithSession.class);
        if (annotation == null) {
            // Check class-level annotation
            annotation = context.getTarget().getClass().getAnnotation(WithSession.class);
        }
        return annotation.value(); // Annotation has default
    }

}
