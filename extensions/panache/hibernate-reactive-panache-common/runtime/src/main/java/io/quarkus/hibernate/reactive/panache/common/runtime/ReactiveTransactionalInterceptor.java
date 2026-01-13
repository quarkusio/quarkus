package io.quarkus.hibernate.reactive.panache.common.runtime;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.REACTIVE_TRANSACTIONAL_METHOD_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.proceedUni;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@ReactiveTransactional
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class ReactiveTransactionalInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Note that intercepted methods annotated with @ReactiveTransactional are validated at build time
        // The build fails if the method does not return Uni
        Context vertxContext = SessionOperations.vertxContext();
        if (vertxContext.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
            return Uni.createFrom().failure(
                    new UnsupportedOperationException(
                            "Cannot call a method annotated with @ReactiveTransactional from a method annotated with @Transactional"));
        }

        // Annotate current method so that we can validate mixing of @ReactiveTransactional with @Transactional
        vertxContext.putLocal(REACTIVE_TRANSACTIONAL_METHOD_KEY, true);

        return SessionOperations.withTransaction(() -> proceedUni(context));
    }

}
