package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;

@WithSessionOnDemand
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class WithSessionOnDemandInterceptor extends AbstractUniInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Note that intercepted methods annotated with @WithSessionOnDemand are validated at build time
        // The build fails if a method does not return Uni
        return SessionOperations.withSessionOnDemand(() -> proceedUni(context));
    }

}
