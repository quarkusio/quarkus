package io.quarkus.hibernate.reactive.panache.common.runtime;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.WITH_TRANSACTION_METHOD_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.proceedUni;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.reactiveInterceptorShouldRun;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WithTransaction
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class WithTransactionInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // Bindings are validated at build time - method-level binding declared on a method that does not return Uni results in a build failure
        // However, a class-level binding implies that methods that do not return Uni are just a no-op
        if (reactiveInterceptorShouldRun()) {
            Context vertxContext = SessionOperations.vertxContext();
            if (vertxContext.getLocal(TRANSACTIONAL_METHOD_KEY) != null) {
                return Uni.createFrom().failure(
                        new UnsupportedOperationException(
                                "Calling a method annotated with @WithTransaction from a method annotated with @Transactional is not supported. "
                                        + "Use either @Transactional or @WithSessionOnDemand/@WithSession/@WithTransaction, "
                                        + "but not both, throughout your whole application."));
            }

            // Annotate current method so that we can validate mixing of @WithTransaction and @Transactional
            vertxContext.putLocal(WITH_TRANSACTION_METHOD_KEY, true);

            WithTransaction withTransaction = getAnnotation(context);
            String persistenceUnitName = withTransaction.value();
            if (withTransaction.stateless()) {
                return SessionOperations.withStatelessTransaction(persistenceUnitName, () -> proceedUni(context))
                        .eventually(() -> clearWithTransactionMethod(vertxContext));
            } else {
                return SessionOperations.withTransaction(persistenceUnitName, () -> proceedUni(context))
                        .eventually(() -> clearWithTransactionMethod(vertxContext));
            }
        }
        return context.proceed();
    }

    private static boolean clearWithTransactionMethod(Context vertxContext) {
        return vertxContext.removeLocal(WITH_TRANSACTION_METHOD_KEY);
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
