package io.quarkus.narayana.jta.runtime.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

/**
 * @author paul.robinson@redhat.com 25/05/2013
 */

@Interceptor
@Transactional(Transactional.TxType.REQUIRED)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptorRequired extends TransactionalInterceptorBase {
    public TransactionalInterceptorRequired() {
        super(false);
    }

    @Override
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new BlockingOperationNotAllowedException("Cannot start a JTA transaction from the IO thread.");
        }
        return super.intercept(ic);
    }

    @Override
    protected Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception {
        if (tx == null) {
            return invokeInOurTx(ic, tm);
        } else {
            return invokeInCallerTx(ic, tx);
        }
    }
}
