package io.quarkus.reactive.transaction.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

@Transactional(Transactional.TxType.REQUIRED)
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class TransactionalInterceptorRequired extends TransactionalInterceptorBase {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        return doIntercept(context);
    }

    @Override
    protected void validateTransactionalType(InvocationContext context) {
        // Required is supported
    }
}
