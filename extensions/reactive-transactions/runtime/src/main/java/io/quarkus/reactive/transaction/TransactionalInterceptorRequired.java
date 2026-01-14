package io.quarkus.reactive.transaction;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

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
    protected void validateTransactionalType(InvocationContext context) {
        // Required is supported
    }
}
