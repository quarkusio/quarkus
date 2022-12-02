package io.quarkus.narayana.jta.runtime.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

/**
 * @author paul.robinson@redhat.com 25/05/2013
 */

@Interceptor
@Transactional(Transactional.TxType.NOT_SUPPORTED)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptorNotSupported extends TransactionalInterceptorBase {
    public TransactionalInterceptorNotSupported() {
        super(true);
    }

    @Override
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return super.intercept(ic);
    }

    @Override
    protected Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception {
        if (tx != null) {
            tm.suspend();
            try {
                return invokeInNoTx(ic);
            } finally {
                tm.resume(tx);
            }
        } else {
            return invokeInNoTx(ic);
        }
    }
}
