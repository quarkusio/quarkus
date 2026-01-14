package io.quarkus.reactive.transaction;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;

@Transactional(Transactional.TxType.NOT_SUPPORTED)
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class TransactionalInterceptorNotSupported extends TransactionalInterceptorBase {

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return doIntercept(ic, null);
    }
}
