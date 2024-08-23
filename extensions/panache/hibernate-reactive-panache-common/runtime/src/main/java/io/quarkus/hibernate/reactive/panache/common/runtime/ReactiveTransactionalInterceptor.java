package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@ReactiveTransactional
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class ReactiveTransactionalInterceptor extends AbstractUniInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Note that intercepted methods annotated with @ReactiveTransactional are validated at build time
        // The build fails if the method does not return Uni
        return SessionOperations.withTransaction(() -> proceedUni(context));
    }

}
