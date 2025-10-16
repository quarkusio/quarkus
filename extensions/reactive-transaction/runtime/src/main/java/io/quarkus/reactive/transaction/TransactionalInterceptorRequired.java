package io.quarkus.reactive.transaction;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

import io.smallrye.mutiny.Uni;

@Transactional(Transactional.TxType.REQUIRED)
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class TransactionalInterceptorRequired extends TransactionalInterceptorBase {

    @Inject
    Instance<AfterWorkStrategy<?>> afterWorkStrategyInstance;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        AfterWorkStrategy<?> afterWorkStrategy = afterWorkStrategyInstance.get();
        if (afterWorkStrategy == null) {
            throw new UnsupportedOperationException(
                    "While using @Transactional we need an AfterWorkStrategy strategy. Something was wrong here");
        }
        return doIntercept(context, afterWorkStrategy);
    }

    @Override
    protected Optional<Uni<Object>> validateTransactionalType(InvocationContext context) {
        // Required is supported
        return Optional.empty();
    }
}
